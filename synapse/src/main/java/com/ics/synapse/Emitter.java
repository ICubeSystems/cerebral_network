package com.ics.synapse;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.event.Event;
import com.ics.nceph.core.message.DocumentStore;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.ProofOfDelivery;
import com.ics.synapse.connector.SynapticConnector;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Jan-2022
 */
public final class Emitter 
{
	private static SynapticConnector connector;
	
	private Emitter()
	{
	}
	
	/**
	 * 
	 * 
	 * @param connector
	 * @return void
	 */
	public static void initiate(SynapticConnector connector)
	{
		if (Emitter.connector == null)
			Emitter.connector = connector;
	}
	
	/**
	 * 
	 * 
	 * @param message
	 * @throws ImproperConnectorInstantiationException
	 * @return void
	 * @throws IOException 
	 */
	public static synchronized void emit(Event event) throws ImproperConnectorInstantiationException, IOException
	{
		// 1. Convert the event to the message object
		Message message = new EventMessage.Builder().event(event).build();
		
		// 2. Create a ProofOfDelivery object and save it to the local DocumentStore. This pod object will be updated when the ack for this message is received from cerebrum.
		//    This pod object will be moved to dynamoDB store once the message is delivered fully (is acknowledged by all the subscribers)
		ProofOfDelivery pod = new ProofOfDelivery.Builder().event(event).messageId(message.decoder().getId()).build();
		DocumentStore.save(pod, message.decoder().getId());
		
		// 3. Get the connection with minimum load
		Connection connection = connector.getConnection();
		// 4. Enqueue the message on the connection to be sent to the Cerebrum
		connection.enqueueMessage(message);
		// 5. Change the interest of the connection to write
		connection.setInterest(SelectionKey.OP_WRITE);
	}
}
