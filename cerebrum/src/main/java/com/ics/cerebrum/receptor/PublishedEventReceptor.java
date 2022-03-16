package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.event.exception.EventNotSubscribedException;
import com.ics.nceph.core.message.DocumentStore;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.ProofOfDelivery;
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
			System.out.println("In Receptor Process:::::::");
			System.out.println("Event ::::: " + getEvent().getEventId() + "-" + getEvent().getObjectJSON());
			
			// 1. Save the event received in the local datastore
			// 1.1 Check if message has already been received
			ProofOfDelivery pod = DocumentStore.load(getMessage().decoder().getId());
			if (pod == null) // If the ProofOfDelivery for the received message is not in the local storage then create a new ProofOfDelivery object for this message
			{
				// Query the dynamoDB to see if the message was fully delivered previously
				// Build ProofOfDelivery object for this message
				pod = new ProofOfDelivery.Builder()
						.event(getEvent())
						.messageId(getMessage().decoder().getId())
						.createdOn(getEvent().getCreatedOn())
						.build();
				pod.setReadRecord(getMessage().getReadRecord());
				// Save the message to the local document store
				DocumentStore.save(pod, getMessage().decoder().getId());
			
				// 2. Send the ACK packet back to the sender notifying that the event has been accepted and the transmission is in progress. 
				//    Sender would maintain & update the event register with the status of the event transmission. 
				//	  Once the subscribers will receive the event then the sender (publisher) will again be notified (needs to be thought on what's the best strategy for that)

				
				// 3. Get the subscriber connectors for this event ()
				ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(getEvent().getEventId());
				
				// 4. Loop over subscriber connectors
				for (Connector connector : subscribers) 
				{
					// 4.1 Using connector's LB get the connection with least load
					Connection connection = connector.getConnection();
					// 4.2 If there are no active connections available in the connector
					if (connection == null)
					{
						// enqueue the message on connectors queue instead of connections queue. When the connections come live these messages would be transferred to the connections queue during the accept phase. 
						connector.enqueueMessage(getMessage());
						// LOG:No connections found for writing. Message [id:xxx] added to the connector's queue [port: xxxx] 
						System.out.println("No connections found for writing. Message [id: " + getMessage().decoder().getMessageId() + "] added to the connector's queue [port: " + connector.getPort() + "]");
						// Move to next subscriber
						continue;
					}
					System.out.println("Connector ["+connector.getPort() + "] - Connection [" + connection.getId() + "]");
					// 4.3 add the event in the eventQueue (ConcurrentLinkedQueue) for transmission (Thread-safe)
					connection.enqueueMessage(getMessage());
					// 4.4 change its selectionKey interest set to write (Thread-safe). Also make sure to do selector.wakeup().
					connection.setInterest(SelectionKey.OP_WRITE);
					System.out.println("Writing Connection " + connection.getId() + ": " + getEvent().getEventId() + "-" + getEvent().getObjectJSON());
				}
			}
			else
			{
				// duplicate message handling
				// Send the ACK packet back to the sender notifying that the event has been accepted. 
			}
		} 
		catch (EventNotSubscribedException e) //ConnectorCluster.getSubscribedConnectors 
		{
			e.printStackTrace();
		} 
		catch (ImproperConnectorInstantiationException e) // connector.getConnection();
		{
			e.printStackTrace();
		}
	}

}
