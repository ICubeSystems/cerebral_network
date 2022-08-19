package com.ics.cerebrum.connector;

import java.io.File;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.MonitorLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;
import com.ics.nceph.core.document.Document;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PorState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
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

		// 2. Check for PODs which are not deleted for more than a specified time
		File messageDirectory = new File(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location"));

		ProcessPOD:
		{
			// 2.1 get all files from the POD directory
			// 2.2 if there are no pods then exit ProcessPOD block
			if (messageDirectory.listFiles() == null) 
				break ProcessPOD;

			// 2.3 Loop over PODs to process
			for (Map.Entry<String, Document> entry : DocumentStore.getCache().entrySet())
			{
				String messageId = entry.getKey();
				ProofOfDelivery pod = (ProofOfDelivery)entry.getValue();
				ProofOfRelay por = null;
				try 
				{
					// if file is older than X minutes and whose state is not finished then resend the message to the another node to make its state to finished
					if(transmissionWindowElapsed(pod))
					{
						if(pod != null && pod.getPortNumber() == connector.getPort()) // Check if the pod was created by the port for which this monitor thread is running
						{
							// Get the subscriber connectors for this event
							ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(pod.getEvent().getEventType());
							// Loop over the subscriber and check the PORs within the POD
							for (Connector subscriberConnector : subscribers) 
							{
								// Get connection from subscriber's connector
								Connection connection = subscriberConnector.getConnection();
								// If there are no active connections in the connector then break.
								if(connection != null) {

									por = pod.getPors().get(subscriberConnector.getPort());
									// If POR exists then check state and process accordingly
									if (por != null)
									{
										// if relay transmissionWindow is not elapsed then do nothing and return
										if (!transmissionWindowElapsed(por))
											return;

										switch (por.getPorState().getState()) 
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
											por.incrementRelayAttempts();
											por.setPorState(PorState.RELAYED);
											DocumentStore.update(pod, messageId);
											break;
										case 300:// ACKNOWLEDGED state of POR
										case 400:// ACK_RECIEVED state of POR
											Message threeWayAckMessage = new AcknowledgeMessage.Builder()
											.data(new ThreeWayAcknowledgementData.Builder()
													.threeWayAckNetworkRecord(new NetworkRecord.Builder()
															.start(new Date().getTime())
															.build()) //ACK_RECEIVED network record with just the start
													.writeRecord(pod.getWriteRecord()) // WriteRecord of PUBLISH_EVENT
													.ackNetworkRecord(pod.getAckNetworkRecord()) // NCEPH_EVENT_ACK network record
													.build())
											.mid(por.getMessageId())
											.type(CerebralOutgoingMessageType.RELAY_ACK_RECEIVED.getMessageType())
											.build();

											enqueueMessage(connection, threeWayAckMessage);
											// Set the RELAY_EVENT attempts
											por.incrementThreeWayAckAttempts();
											por.setPorState(PorState.ACK_RECIEVED);
											DocumentStore.update(pod, messageId);
											break;
										case 500:// FINISHED state of POR
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
										por.incrementRelayAttempts();

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
							}
						}
					}
				}
				catch (MessageBuildFailedException e) 
				{
					// Log
					NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
							.messageId(messageId)
							.action(por.getPorState().getState() == 100 || por.getPorState().getState() == 200 ? "NCEPH_EVENT build failed":"ACK_RECEIVED build failed")
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
								.description(por.getPorState().getState() == 100 || por.getPorState().getState() == 200 ? "Relay":"ThreeWayAck"+" counter decrement failed after MessageBuildFailedException")
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
}