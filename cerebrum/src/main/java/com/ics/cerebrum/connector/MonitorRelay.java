package com.ics.cerebrum.connector;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.MessageLog;
import com.ics.logger.MonitorLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.MonitorTask;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.event.exception.EventNotSubscribedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.message.data.ThreeWayAcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.util.ByteUtil;

import lombok.Builder;

/**
 * This consumer/ function class to process {@link ProofOfPublish} documents to manage the relay of the message
 * 
 * @author Anshul
 * @version 1.0
 * @since Oct 17, 2022
 */
@Builder
public class MonitorRelay extends MonitorTask
{
	private CerebralConnector connector;
	
	@Override
	public void accept(Entry<String, ProofOfPublish> t)
	{
		processPods(t);
	}
	
	/**
	 * Check for uncompleted relay messages and process them by re-sending them according to their state 
	 * @throws ImproperMonitorInstantiationException
	 * @throws ImproperConnectorInstantiationException
	 * @since Oct 17, 2022
	 */
	private void processPods(Map.Entry<String, ProofOfPublish> entry) 
	{
		String messageId = entry.getKey();
		ProofOfPublish pod = entry.getValue();
		ProofOfRelay por = null;
		try 
		{
			// if file is older than X minutes and whose state is not finished then resend the message to the another node to make its state to finished
			if(transmissionWindowElapsed(pod)) // Check if the pod was created by the port for which this monitor thread is running
			{
				// Get the subscriber connectors for this event
				ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(pod.getEvent().getEventType());
				// Loop over the subscriber and check the PORs within the POD
				for (Connector subscriberConnector : subscribers) 
				{
					// Get connection from subscriber's connector
					Connection connection;
					try
					{
						connection = subscriberConnector.getConnection();
					} catch (ImproperConnectorInstantiationException e){continue;}
					
					por = ProofOfRelay.load(connector.getPort(), subscriberConnector.getPort(), messageId);

					// If there are no active connections in the connector then break.
					if(connection != null) 
					{
						// If POR exists then check state and process accordingly
						if (por != null)
						{
							switch (por.getMessageDeliveryState()) 
							{
							case 100:// INITIAL state of POR
							case 200:// RELAYED state of POR
								// Build the EventMessage from POD
								Message eventMessage = new EventMessage.Builder()
								.type(CerebralOutgoingMessageType.RELAY_EVENT.getMessageType())
								.event(pod.getEvent())
								.mid(por.getMessageId())
								.originatingPort(connector.getPort())
								.buildAgain();

								enqueueMessage(connection, eventMessage);
								// Set the RELAY_EVENT attempts
								por.incrementEventMessageAttempts();
								por.setMessageDeliveryState(MessageDeliveryState.DELIVERED.getState());
								DocumentStore.getInstance().update(pod, messageId);
								break;
							case 300:// ACKNOWLEDGED state of POR
							case 400:// ACK_RECIEVED state of POR
								// if relay transmissionWindow is not elapsed then do nothing and return
								if (!transmissionWindowElapsed(por))
									break;

								Message threeWayAckMessage = new AcknowledgeMessage.Builder()
										.data(new ThreeWayAcknowledgementData.Builder()
										.threeWayAckNetworkRecord(new NetworkRecord.Builder()
												.start(new Date().getTime())
												.build()) //ACK_RECEIVED network record with just the start
										.writeRecord(pod.getEventMessageWriteRecord()) // WriteRecord of PUBLISH_EVENT
										.ackNetworkRecord(pod.getAckMessageNetworkRecord()) // NCEPH_EVENT_ACK network record
										.build())
								.mid(por.getMessageId())
								.originatingPort(ByteUtil.convertToByteArray(connector.getPort(), 2))
								.type(CerebralOutgoingMessageType.RELAY_ACK_RECEIVED.getMessageType())
								.build();

								enqueueMessage(connection, threeWayAckMessage);
								// Set the RELAY_EVENT attempts
								por.incrementThreeWayAckMessageAttempts();
								por.setMessageDeliveryState(MessageDeliveryState.ACK_RECIEVED.getState());
								DocumentStore.getInstance().update(pod, messageId);
								break;
							case 500:// FINISHED state of POR
								pod.finished();
								break;
							default:
								break;
							}
						}
						else if(!pod.getSubscribedPorts().contains(subscriberConnector.getPort())) // If POR does not exists then create a new POR and relay to the missing subscriber
						{
							// Create a new POR and relay the message to this subscriber
							por = new ProofOfRelay.Builder()
									.relayedOn(new Date().getTime())
									.messageId(messageId)
									.consumerPort(subscriberConnector.getPort())
									.producerPort(connector.getPort())
									.build();

							// Set the RELAY_EVENT attempts
							por.incrementEventMessageAttempts();
							pod.addSubscribedPort(subscriberConnector.getPort());
							// Save the POD
							DocumentStore.getInstance().update(por, messageId);
							DocumentStore.getInstance().update(pod, messageId);
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
		catch (MessageBuildFailedException e) 
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(messageId)
					.action(por.getMessageDeliveryState() == 100 || por.getMessageDeliveryState() == 200 ? "NCEPH_EVENT build failed":"ACK_RECEIVED build failed")
					.description("message build failed in monitor")
					.logError(),e);
			por.decrementAttempts();
			//IOException Save the POD
			try 
			{
				DocumentStore.getInstance().update(pod, pod.getMessageId());
			} 
			catch (DocumentSaveFailedException e1) 
			{
				//Log
				NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
						.messageId(String.valueOf(pod.getMessageId()))
						.action("Pod updation failed")
						.description(por.getMessageDeliveryState() == 100 || por.getMessageDeliveryState() == 200 ? "Relay":"ThreeWayAck"+" counter decrement failed after MessageBuildFailedException")
						.logError(), e1);
			}
		} 
		catch (DocumentSaveFailedException e) {} // Logging for this exception is already handled in DocumentStore.update() method
		catch (EventNotSubscribedException e) {
			NcephLogger.MONITOR_LOGGER.fatal(new MonitorLog.Builder()
					.monitorPort(connector.getPort())
					.action("Event Not Subscribed")
					.logInfo());
		}
	}
	
}
