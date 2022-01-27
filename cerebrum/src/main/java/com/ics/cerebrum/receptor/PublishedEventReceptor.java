package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.event.exception.EventNotSubscribedException;
import com.ics.nceph.core.message.Message;
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
			
			// 1. Save the event received in the local datastore (not yet decided - most likely it will be AWS dynamoDB)
			// 2. Get the subscriber connectors for this event ()
			ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(getEvent().getEventId());
			
			// 3. Loop over subscriber connectors
			for (Connector connector : subscribers) 
			{
				// 3.1 Using connector's LB get the connection with least load
				Connection connection = connector.getConnection();
				// 3.2 If there are no active connections available in the connector
				if (connection == null)
				{
					// enqueue the message on connectors queue instead of connections queue. When the connections come live these messages would be transferred to the connections queue during the accept phase. 
					connector.enqueueMessage(getMessage());
					// LOG:No connections found for writing. Message [id:xxx] added to the connector's queue [port: xxxx] 
					System.out.println("No connections found for writing. Message [id: " + getMessage().decoder().getId() + "] added to the connector's queue [port: " + connector.getPort() + "]");
					// Move to next subscriber
					continue;
				}
				System.out.println("Connector ["+connector.getPort() + "] - Connection [" + connection.getId() + "]");
				// 3.3 add the event in the eventQueue (ConcurrentLinkedQueue) for transmission (Thread-safe)
				connection.enqueueMessage(getMessage());
				// 3.4 change its selectionKey interest set to write (Thread-safe). Also make sure to do selector.wakeup().
				connection.setInterest(SelectionKey.OP_WRITE);
				System.out.println("Writing Connection " + connection.getId() + ": " + getEvent().getEventId() + "-" + getEvent().getObjectJSON());
			}
			
			// 4. Send the ACK packet back to the sender notifying that the event has been accepted and the transmission is in progress. 
			//    Sender would maintain & update the event register with the status of the event transmission. 
			//	  Once the subscribers will receive the event then the sender (publisher) will again be notified (needs to be thought on what's the best strategy for that)
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
