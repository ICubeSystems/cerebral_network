package com.ics.cerebrum.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.event.Acknowledgement;
import com.ics.nceph.core.event.exception.EventNotSubscribedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
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
		try 
		{
			// 1. Save the message received in the local datastore
			// 1.1 Check if message has already been received
			ProofOfDelivery pod = (ProofOfDelivery) DocumentStore.load(getMessage().decoder().getId());
			
			if (pod == null) // If the ProofOfDelivery for the received message is not in the local storage then create a new ProofOfDelivery object for this message
			{
				// TODO: Query the dynamoDB to see if the message was fully delivered previously [TBD]
				// Create ProofOfDelivery object for this message
				pod = new ProofOfDelivery.Builder()
						.event(getEvent())
						.messageId(getMessage().decoder().getId())
						.createdOn(getEvent().getCreatedOn())
						.build();
				pod.setReadRecord(getMessage().getReadRecord());
				pod.incrementAcknowledgementAttempts();
				
				// Save the POD in local storage
				try {
					DocumentStore.save(pod, getMessage().decoder().getId());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				// 2. Send the ACK message (NCEPH_EVENT_ACK) back to the sender notifying that the event has been accepted and the transmission is in progress. 
				try 
				{
					NetworkRecord networkRecord = new NetworkRecord.Builder().start(new Date()).build();
					pod.setAckNetworkRecord(networkRecord);
					// 2.1 Create NCEPH_EVENT_ACK message 
					Message message = new AcknowledgeMessage.Builder()
										.data(new Acknowledgement.Builder()
												.readRecord(getMessage().getReadRecord())
												.ackNetworkRecord(networkRecord)
												.build())
										.messageId(getMessage().getMessageId())
										.type(CerebralOutgoingMessageType.NCEPH_EVENT_ACK.getMessageType())
										.sourceId(getMessage().getSourceId())
										.build();
					
					// 2.2 Enqueue NCEPH_EVENT_ACK for sending
					getIncomingConnection().enqueueMessage(message);
					getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
				} catch (IOException e) 
				{
					// Log
					NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
							.messageId(getMessage().decoder().getId())
							.action("NCEPH_EVENT_ACK build failed")
							.logError(),e);
					// TODO: Undo the above POD updates (TBD - may be not required)
				}
				
				// Begin RELAY
				// Change the type of the message to RELAY_EVENT
				getMessage().setType((byte)0x0B);
				ConcurrentHashMap<Integer, ProofOfRelay> porHashMap = new ConcurrentHashMap<Integer, ProofOfRelay>();
				pod.setPors(porHashMap);
				
				// 3. Get the subscriber connectors for this event
				ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(getEvent().getEventId());
				pod.setSubscriberCount(subscribers.size());
				
				// 4. Loop over subscriber connectors
				for (Connector connector : subscribers) 
				{
					ProofOfRelay por = new ProofOfRelay.Builder()
										.relayedOn(new Date())
										.messageId(getMessage().decoder().getId())
										.build();
					porHashMap.put(connector.getPort(), por);
					
					// Save the POD
					try {
						DocumentStore.save(pod, getMessage().decoder().getId());
					} catch (IOException e) {}

						// 4.1 Using connector's LB get the connection with least load
					Connection connection = connector.getConnection();
					
					// 4.2 If there are no active connections available in the connector
					if (connection == null)
					{
						// enqueue the message on connectors queue instead of connections queue. When the connections come live these messages would be transferred to the connections queue during the accept phase. 
						connector.enqueueMessage(getMessage());
						// Move to next subscriber
						continue;
					}
					// 4.3 add the event in the eventQueue (ConcurrentLinkedQueue) for transmission (Thread-safe)
					connection.enqueueMessage(getMessage());
					// 4.4 change its selectionKey interest set to write (Thread-safe). Also make sure to do selector.wakeup().
					connection.setInterest(SelectionKey.OP_WRITE);
				}
			}
			else
			{
				// duplicate message handling - TBD
				// If ACK_RECEIVED message is not received then send the NCEPH_EVENT_ACK_AGAIN
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
