package com.ics.cerebrum.connector;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.ConnectionLog;
import com.ics.logger.DynamodbLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.MonitorLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;
import com.ics.nceph.core.db.dynamoDB.entity.PublishedMessageEntity;
import com.ics.nceph.core.db.dynamoDB.entity.ReceivedMessageEntity;
import com.ics.nceph.core.db.dynamoDB.exception.DatabaseException;
import com.ics.nceph.core.db.dynamoDB.repository.PublishedMessageRepository;
import com.ics.nceph.core.db.dynamoDB.repository.ReceivedMessageRepository;
import com.ics.nceph.core.document.Document;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.MessageDeliveryState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.ProofOfPublish;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.event.exception.EventNotSubscribedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.message.data.ThreeWayAcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;

/**
 * This is a thread class which is responsible for continuous monitoring of the messages flowing through cerebrum. 
 * This class provides 100% RELIABILITY to the cerebral network, i.e - <b>guaranteed relay of all the messages to all the subscribers</b><br>
 * One monitor thread per connecter is created at the time of cerebral bootstrapping. <br>
 * 
 * Following tasks are performed by the monitor to achieve reliability:
 * <ol>
 * 	<li>If there are any messages in the connector's relay queue then transfer them to connection's relay queue for transmission</li>
 * 	<li>Check for PODs which are not deleted for more than a specified time and process them as per their POR states:</li>
 * 		<ul>
 * 			<li>INITIAL | RELAYED: re-send RELAY_EVENT message</li>
 * 			<li>ACKNOWLEDGED | ACK_RECIEVED: re-send RELAY_ACK_RECEIVED message</li>
 * 			<li>FINISHED: Move the POD to global persistent storage (DynamoDB)</li>
 * 		</ul>
 * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Jan-2022
 */
public class CerebralMonitor extends ConnectorMonitorThread 
{
	private PublishedMessageRepository publishedMessageRepository;

	private ReceivedMessageRepository relayedMessageRepository ;

	public CerebralMonitor() {}

	/**
	 * The DB repositories are manually injected via this constructor from the connectorCulsterInitializer. 
	 * TODO: This should be done using spring container instead.
	 * 
	 * @param publishedMessageRepository
	 * @param relayedmessageRepository
	 */
	public CerebralMonitor(PublishedMessageRepository publishedMessageRepository, ReceivedMessageRepository relayedmessageRepository) 
	{
		this.publishedMessageRepository = publishedMessageRepository;
		this.relayedMessageRepository = relayedmessageRepository;
	}

	@Override
	public void monitor() throws ImproperMonitorInstantiationException, ImproperConnectorInstantiationException 
	{
		CerebralConnector connector = (CerebralConnector) getConnector();
		NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
				.monitorPort(connector.getPort())
				.action("Cerebral monitor start")
				.logInfo());
		// 1. Check if there are any messages in the connector's relay queue. Transfer them to connection's relay queue for transmission.
		if (connector.getRelayQueue().size() > 0 && connector.getActiveConnections().size()>0)
		{
			NcephLogger.MONITOR_LOGGER.info(new ConnectionLog.Builder()
					.action("Transfer relay queue")
					.data(new LogData()
							.entry("Relay size", String.valueOf(connector.getRelayQueue().size()))
							.toString())
					.logInfo());
			Connection connection = null;
			while(!connector.getRelayQueue().isEmpty()) 
			{
				connection = connector.getConnection();
				Message message = connector.getRelayQueue().poll();
				connection.enqueueMessage(message, QueuingContext.QUEUED_FROM_MONITOR);
				// remove message from connectorQueuedUpMessageRegister 
				getConnector().removeConnectorQueuedUpMessage(message);
				connection.setInterest(SelectionKey.OP_WRITE);
			}
		}


		ProcessPOD:
		{
			// 2.1 get all files from the POD directory
			// 2.2 if there are no pods then exit ProcessPOD block
			if (DocumentStore.getCache().isEmpty()) 
				break ProcessPOD;

			// 2.3 Loop over PODs to process
			for (Map.Entry<String, Document> entry : DocumentStore.getCache().entrySet())
			{
				String messageId = entry.getKey();
				ProofOfPublish pod = (ProofOfPublish)entry.getValue();
				ProofOfRelay por = null;
				try 
				{
					// if file is older than X minutes and whose state is not finished then resend the message to the another node to make its state to finished
					if(pod != null && connector.getPort().equals(pod.getProducerPortNumber()))
					{
						if(transmissionWindowElapsed(pod)) // Check if the pod was created by the port for which this monitor thread is running
						{
							// Get the subscriber connectors for this event
							ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(pod.getEvent().getEventType());
							// Loop over the subscriber and check the PORs within the POD
							for (Connector subscriberConnector : subscribers) 
							{

								// Get connection from subscriber's connector
								Connection connection = subscriberConnector.getConnection();

								por = pod.getPors().get(subscriberConnector.getPort());

								// If there are no active connections in the connector then break.
								if(connection != null) 
								{
									// If POR exists then check state and process accordingly
									if (por != null)
									{
										// if relay transmissionWindow is not elapsed then do nothing and return
										if (!transmissionWindowElapsed(por))
											return;

										switch (por.getMessageDeliveryState().getState()) 
										{
										case 100:// INITIAL state of POR
										case 200:// RELAYED state of POR
											// Build the EventMessage from POD
											Message eventMessage = new EventMessage.Builder()
											.type(CerebralOutgoingMessageType.RELAY_EVENT.getMessageType())
											.event(pod.getEvent())
											.mid(por.getMessageId())
											.buildAgain();

											enqueueMessage(connection, eventMessage);
											// Set the RELAY_EVENT attempts
											por.incrementEventMessageAttempts();
											por.setMessageDeliveryState(MessageDeliveryState.DELIVERED);
											DocumentStore.update(pod, messageId);
											break;
										case 300:// ACKNOWLEDGED state of POR
										case 400:// ACK_RECIEVED state of POR
											Message threeWayAckMessage = new AcknowledgeMessage.Builder()
											.data(new ThreeWayAcknowledgementData.Builder()
													.threeWayAckNetworkRecord(new NetworkRecord.Builder()
															.start(new Date().getTime())
															.build()) //ACK_RECEIVED network record with just the start
													.writeRecord(pod.getEventMessageWriteRecord()) // WriteRecord of PUBLISH_EVENT
													.ackNetworkRecord(pod.getAckMessageNetworkRecord()) // NCEPH_EVENT_ACK network record
													.build())
											.mid(por.getMessageId())
											.type(CerebralOutgoingMessageType.RELAY_ACK_RECEIVED.getMessageType())
											.build();

											enqueueMessage(connection, threeWayAckMessage);
											// Set the RELAY_EVENT attempts
											por.incrementThreeWayAckMessageAttempts();
											por.setMessageDeliveryState(MessageDeliveryState.ACK_RECIEVED);
											DocumentStore.update(pod, messageId);
											break;
										case 500:// FINISHED state of POR
											// Call savePORInDB method to save receive message in the DynamoDB
											savePORInDB(pod, por);
											break;
										default:
											break;
										}
									}
									else // If POR does not exists then create a new POR and relay to the missing subscriber
									{
										// Create a new POR and relay the message to this subscriber
										por = new ProofOfRelay.Builder()
												.relayedOn(new Date().getTime())
												.messageId(messageId)
												.build();
										pod.addPor(subscriberConnector.getPort(), por);

										// Set the RELAY_EVENT attempts
										por.incrementEventMessageAttempts();

										// Save the POD
										DocumentStore.update(pod, messageId);

										// Convert the event to the message object
										Message eventMessage = new EventMessage.Builder()
												.type(CerebralOutgoingMessageType.RELAY_EVENT.getMessageType())
												.event(pod.getEvent())
												.mid(por.getMessageId())
												.buildAgain();

										enqueueMessage(connection, eventMessage);
									}
								} 
								else {
									// Call savePORInDB method to save receive message in the DynamoDB
									savePORInDB(pod, por);
								}
							}
							// Call savePODInDB method to save publish message in the DynamoDB
							savePODInDB(pod);

							// Delete POD in local store
							deletePod(pod);
						}
					}					
				}
				catch (MessageBuildFailedException e) 
				{
					// Log
					NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
							.messageId(messageId)
							.action(por.getMessageDeliveryState().getState() == 100 || por.getMessageDeliveryState().getState() == 200 ? "NCEPH_EVENT build failed":"ACK_RECEIVED build failed")
							.description("message build failed in monitor")
							.logError(),e);
					por.decrementAttempts();
					//IOException Save the POD
					try 
					{
						DocumentStore.update(pod, pod.getMessageId());
					} 
					catch (DocumentSaveFailedException e1) 
					{
						//Log
						NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
								.messageId(String.valueOf(pod.getMessageId()))
								.action("Pod updation failed")
								.description(por.getMessageDeliveryState().getState() == 100 || por.getMessageDeliveryState().getState() == 200 ? "Relay":"ThreeWayAck"+" counter decrement failed after MessageBuildFailedException")
								.logError(), e1);
					}
				} 
				catch (DocumentSaveFailedException e) {} // Logging for this exception is already handled in DocumentStore.update() method
				catch (EventNotSubscribedException e) {
					NcephLogger.MONITOR_LOGGER.fatal(new MonitorLog.Builder()
							.monitorPort(connector.getPort())
							.action("Subscription Not Subscribed")
							.logInfo());
				}
			}
		}

		NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
				.monitorPort(connector.getPort())
				.action("Cerebral monitor end")
				.logInfo());
	}
	
	private boolean isReadyToUpload(ProofOfDelivery pod) {
		return NcephConstants.saveInDB 
				&& !pod.isMessageInDB() 
				&& pod.getMessageDeliveryState().getState() == MessageDeliveryState.FINISHED.getState();
	}
	
	/**
	 * 
	 * @param pod
	 * @param por
	 * @throws DocumentSaveFailedException
	 */
	private void savePORInDB(ProofOfPublish pod, ProofOfRelay por)
	{
		try 
		{
			if(isReadyToUpload(por)) 
			{
				saveReceiveMessage(por, pod.getEvent());
				por.setMessageInDB(true);
				DocumentStore.update(pod, pod.getMessageId());	
			}
		} catch (DatabaseException e) {
			NcephLogger.DYNAMODB_LOGGER.fatal(new DynamodbLog.Builder()
					.action("DatabaseException")
					.description("Receive Message save failed: " + e.getMessage())
					.logError(),e);
		} catch (DocumentSaveFailedException e) {
			NcephLogger.DYNAMODB_LOGGER.fatal(new DynamodbLog.Builder()
					.action("POR: MessageInDB update Failed")
					.description(e.getMessage())
					.logError(),e);
		}
	}

	private void savePODInDB(ProofOfPublish pop) 
	{
		try 
		{
			if(isReadyToUpload(pop)) 
			{
				savePublishedMessage(pop);
				pop.setMessageInDB(true);
				DocumentStore.update(pop, pop.getMessageId());	
			}

		} catch (DatabaseException e) {
			NcephLogger.DYNAMODB_LOGGER.fatal(new DynamodbLog.Builder()
					.action("DatabaseException")
					.description("Publish Message save failed: " + e.getMessage())
					.logError(),e);
		} catch (DocumentSaveFailedException e) {
			NcephLogger.DYNAMODB_LOGGER.fatal(new DynamodbLog.Builder()
					.action("POD: MessageInDB update Failed")
					.description(e.getMessage())
					.logError(),e);
		}
	}

	/**
	 * Save Publish message in DynamoDB
	 * 
	 * @param pop
	 * @throws DatabaseException
	 */
	public void savePublishedMessage(ProofOfPublish pop) throws DatabaseException
	{
		try 
		{
			PublishedMessageEntity publishMessage = new PublishedMessageEntity.Builder()
					.pod(pop.toString())
					.build();
			// Save data in the DynamoDB
			publishedMessageRepository.save(publishMessage);
		} catch (ResourceNotFoundException | DynamoDBMappingException | JsonProcessingException e) {
			throw new DatabaseException("Publish message save failed Exception " , e);
		}
	}

	/**
	 * Save Receive message in DynamoDB
	 * 
	 * @param por
	 * @throws DatabaseException
	 */
	public void saveReceiveMessage(ProofOfRelay por, EventData eventData) throws DatabaseException
	{
		try 
		{
			ReceivedMessageEntity receiveMessage = new ReceivedMessageEntity.Builder()
					.por(por.toString())
					.event(eventData)
					.build();

			// Save data in the DynamoDB
			relayedMessageRepository.save(receiveMessage);
		} catch (ResourceNotFoundException | DynamoDBMappingException | JsonProcessingException e) {
			throw new DatabaseException("Receive message save failed Exception " , e);
		}
	}

	/**
	 * Delete pod in local store
	 * 
	 * @param pod
	 */
	private void deletePod(ProofOfPublish pod) 
	{
		if(pod.isMessageInDB() && pod.getSubscriberCount() == pod.getRelayCount().intValue() && !DocumentStore.delete(pod.getMessageId(), pod)) 
		{
			// Pod not deleted ?
		}
		
	}
}
