package com.ics.synapse;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import com.ics.env.Environment;
import com.ics.id.exception.IdGenerationFailedException;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.synapse.connector.SynapticConnector;
import com.ics.synapse.exception.EmitException;
import com.ics.util.OSInfo;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Jan-2022
 */
public final class Emitter extends OSInfo 
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
	 * @throws EmitException 
	 * @throws IdGenerationFailedException 
	 */

	public static void emit(EventData event) throws EmitException, IdGenerationFailedException
	{
		// 1. Convert the event to the message object
		Message message;
		try 
		{
			message = new EventMessage.Builder().event(event).build();
		} catch (MessageBuildFailedException e) {
			throw new EmitException("Message build falied", e);
		}

		// Log
		NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
				.messageId(message.decoder().getId())
				.action("EMIT")
				.data(
						new LogData()
						.entry("eventId", String.valueOf(event.getEventId()))
						.entry("createdOn", String.valueOf(event.getCreatedOn()))
						.toString())
				.logInfo());
		
		// 2. Create a ProofOfDelivery object and save it to the local DocumentStore. This pod object will be updated when the ack for this message is received from cerebrum.
		//    This pod object will be moved to dynamoDB store once the message is delivered fully (is acknowledged by all the subscribers)
		ProofOfDelivery pod = new ProofOfDelivery.Builder()
				.event(event)
				.messageId(message.decoder().getId())
				.createdOn(event.getCreatedOn())
				.portNumber(connector.getPort())
				.build();
		// 2.1 Set the PUBLISH_EVENT attempts
		pod.incrementPublishAttempts();
		
		try 
		{
			DocumentStore.save(pod, message.decoder().getId());
			// MOCK CODE: to test the reliable delivery of the messages
			if(Environment.isDev() && pod.getMessageId().equals("1-12")) 
			{
				System.out.println("forceStop"+message.decoder().getId());
				return;
			}
			// END MOCK CODE
		} catch (DocumentSaveFailedException e) {
			throw new EmitException("POD creation/ save failed", e);
		}

		// 3. Get the connection with minimum load
		Connection connection;
		try 
		{
			connection = connector.getConnection();
			
			if (connection == null)
			{
				// enqueue the message on connectors queue instead of connections queue. When the connections come live these messages would be transferred to the connections queue during the accept phase. 
				connector.enqueueMessage(message);
				return;
			} 
			
			// 4. Enqueue the message on the connection to be sent to the Cerebrum
			connection.enqueueMessage(message, QueuingContext.QUEUED_FROM_EMITTER);

			// 5. Change the interest of the connection to write
			connection.setInterest(SelectionKey.OP_WRITE);
		} catch (ImproperConnectorInstantiationException e) {
			throw new EmitException("Connector instantiation improper", e);
		}
		
	}
}
