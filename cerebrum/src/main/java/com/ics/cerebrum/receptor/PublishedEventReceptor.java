package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Date;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.env.Environment;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PodState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.event.exception.EventNotSubscribedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.EventReceptor;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 05-Jan-2022
 */
public class PublishedEventReceptor extends EventReceptor 
{
	
	public PublishedEventReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// Log
		NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
				.messageId(getMessage().decoder().getId())
				.action("Event Recieved")
				.data(new LogData()
						.entry("eventId", String.valueOf(getEvent().getEventId()))
						.entry("createdOd", String.valueOf(getEvent().getCreatedOn()))
						.toString())
				.logInfo());
		try 
		{
			// 1. Save the message received in the local datastore
			// 1.1 Check if message has already been received
			ProofOfDelivery pod = (ProofOfDelivery) DocumentStore.load(getMessage().decoder().getId());
			if (pod == null) 
			{
				// TODO: Query the dynamoDB to see if the message was fully delivered previously [TBD]
				try 
				{
					// Create ProofOfDelivery object for this message
					pod = new ProofOfDelivery.Builder()
							.event(getEvent())
							.messageId(getMessage().decoder().getId())
							.createdOn(getEvent().getCreatedOn())
							.portNumber(getIncomingConnection().getConnector().getPort())
							.build();
					// 2. Update POD
					// 2.1 Set PUBLISH_EVENT read record
					pod.setReadRecord(getMessage().getReadRecord());
					// 2.2 Set PUBLISH_EVENT network record
					pod.setEventNetworkRecord(buildNetworkRecord());
					// 2.4 Set the PUBLISH_EVENT attempts
					pod.incrementPublishAttempts();
					// 2.4 Set the acknowledgement attempts
					pod.incrementAcknowledgementAttempts();
					// 2.6 Set POD State to PUBLISHED
					pod.setPodState(PodState.PUBLISHED);
					// Save the POD in local storage
					DocumentStore.save(pod, getMessage().decoder().getId());
					
					// 3. Send the ACK message (NCEPH_EVENT_ACK) back to the sender notifying that the event has been accepted and the transmission is in progress. 
					// 3.1 Create NCEPH_EVENT_ACK message 		
					Message message = new AcknowledgeMessage.Builder()
							.data(
									new AcknowledgementData.Builder()
									.readRecord(getMessage().getReadRecord())
									.eventNetworkRecord(pod.getEventNetworkRecord())
									.build())
							.messageId(getMessage().getMessageId())
							.type(CerebralOutgoingMessageType.NCEPH_EVENT_ACK.getMessageType())
							.sourceId(getMessage().getSourceId())
							.build();
					// 3.2 Enqueue NCEPH_EVENT_ACK for sending
					getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
					getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
				} 
				catch (DocumentSaveFailedException e){
					return;
				} 
				catch (MessageBuildFailedException e) 
				{
					// Log
					NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
							.messageId(getMessage().decoder().getId())
							.action("NCEPH_EVENT_ACK build failed")
							.logError(),e);
					// decrement acknowledgement attempts in the pod		
					pod.decrementAcknowledgementAttempts();
					// Save the POD
					try 
					{
						DocumentStore.update(pod, getMessage().decoder().getId());
					} 
					catch (DocumentSaveFailedException e1){}
					return;
				}
				
				// Begin RELAY
				// Change the type of the message to RELAY_EVENT
				getMessage().setType(CerebralOutgoingMessageType.RELAY_EVENT.getMessageType());
				// 4. Get the subscriber connectors for this event
				ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(getEvent().getEventId());
				pod.setSubscriberCount(subscribers.size());

				// 5. Loop over subscriber connectors
				for (Connector connector : subscribers) 
				{
					try 
					{
						ProofOfRelay por = new ProofOfRelay.Builder()
								.relayedOn(new Date().getTime())
								.messageId(getMessage().decoder().getId())
								.build();
						// Set the RELAY_EVENT attempts
						por.incrementRelayAttempts();
						pod.addPor(connector.getPort(), por);

						// Save the POD
						DocumentStore.update(pod, getMessage().decoder().getId());

						// MOCK CODE: to test the reliable delivery of the messages
						if(Environment.isDev() && por.getMessageId().equals("1-11")) 
						{
							System.out.println("forceStop"+getMessage().decoder().getId());
							continue;
						}
						// END MOCK CODE

						// 5.1 Using connector's LB get the connection with least load
						Connection connection = connector.getConnection();

						// 5.2 If there are no active connections available in the connector
						if (connection == null)
						{
							// enqueue the message on connectors queue instead of connections queue. When the connections come live these messages would be transferred to the connections queue during the accept phase. 
							connector.enqueueMessage(getMessage());
							// Move to next subscriber
							continue;
						}
						// 5.3 add the event in the eventQueue (ConcurrentLinkedQueue) for transmission (Thread-safe)
						connection.enqueueMessage(getMessage(), QueuingContext.QUEUED_FROM_RECEPTOR);
						
						// MOCK CODE: to test the no duplicate delivery of the messages
						if(Environment.isDev() && por.getMessageId().equals("1-15")) 
						{
							connection.enqueueMessage(getMessage(), QueuingContext.QUEUED_FROM_RECEPTOR);
						}
						// END MOCK CODE
						
						// 5.4 change its selectionKey interest set to write (Thread-safe). Also make sure to do selector.wakeup().
						connection.setInterest(SelectionKey.OP_WRITE);
					}
					catch (DocumentSaveFailedException e) {}
				}
			}
			else
			{
				// duplicate message handling - TBD
				// If ACK_RECEIVED message is not received then send the NCEPH_EVENT_ACK
				// If ACK_RECEIVED message is received then send DELETE_POD
			}
		} 
		catch (EventNotSubscribedException e) //ConnectorCluster.getSubscribedConnectors 
		{
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("EventNotSubscribedException")
					.logError(),e);
		} 
		catch (ImproperConnectorInstantiationException e) // connector.getConnection();
		{
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("ImproperConnectorInstantiationException")
					.logError(),e);
		}
	}

}
