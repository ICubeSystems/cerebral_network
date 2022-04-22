package com.ics.synapse;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.event.Event;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
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
	 * @param message
	 * 
	 * @throws ImproperConnectorInstantiationException
	 * @return void
	 * @throws IOException 
	 */
	public static void emit(Event event) throws ImproperConnectorInstantiationException, IOException
	{
		// 1. Convert the event to the message object
		Message message = new EventMessage.Builder().event(event).build();
		
		// 2. Create a ProofOfDelivery object and save it to the local DocumentStore. This pod object will be updated when the ack for this message is received from cerebrum.
		//    This pod object will be moved to dynamoDB store once the message is delivered fully (is acknowledged by all the subscribers)
		ProofOfDelivery pod = new ProofOfDelivery.Builder()
				.event(event)
				.messageId(message.decoder().getId())
				.createdOn(event.getCreatedOn())
				.build();
		DocumentStore.save(pod, message.decoder().getId());

		// 3. Get the connection with minimum load
		Connection connection = connector.getConnection();
		if (connection == null)
		{
			// enqueue the message on connectors queue instead of connections queue. When the connections come live these messages would be transferred to the connections queue during the accept phase. 
			connector.enqueueMessage(message);
			// LOG: No connections found for writing. Message [id:xxx] added to the connector's queue [port: xxxx] 
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(message.decoder().getId())
					.action("Emit")
					.description("No connections found for writing. POD created - " + message.decoder().getId() + ".json & enqueued to connector's relayQueue")
					.logInfo());
			return;
		}
		// 4. Enqueue the message on the connection to be sent to the Cerebrum
		connection.enqueueMessage(message);
		// 5. Change the interest of the connection to write
		connection.setInterest(SelectionKey.OP_WRITE);
		// Log
		NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
				.messageId(message.decoder().getId())
				.action("Emit")
				.description("POD created - " + message.decoder().getId() + ".json & enqueued to id:" + connection.getId())
				.logInfo());
	}
}
