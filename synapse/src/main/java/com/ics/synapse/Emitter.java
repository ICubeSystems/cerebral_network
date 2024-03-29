package com.ics.synapse;

import java.nio.channels.SelectionKey;

import com.ics.env.Environment;
import com.ics.id.exception.IdGenerationFailedException;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.synapse.connector.SynapticConnector;
import com.ics.synapse.exception.EmitException;

/**
 * Final class to be used by the synaptic applications to emit an event on the nceph network.
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
	 * Static method to emit an event on the nceph network.
	 * Following is the process to emit an event:
	 * <ol>
	 * 	<li>Convert the {@link EventData} to the {@link Message} object</li>
	 * 	<li>Create a {@link ProofOfPublish ProofOfPublish} (POD) and save it to the local document store. 
	 * 		The pod is used to track the status of the delivery of the message (on the synapse side the POD only keeps track of delivery to cerebrum) </li>
	 * 	<li>Get a connection with least load from the connector's load balancer ({@link Connector#getConnection()}) </li>
	 * 	<li>If no active connection is available on the connector then {@link Connector#enqueueMessage(Message) enqueue} the message on the connector's relay {@link Connector#relayQueue queue} and <code>return</code></li>
	 * 	<li>If connection is available then {@link Connection#enqueueMessage(Message, QueuingContext) enqueue} the message on the connection's relay {@link Connection#relayQueue queue} and set the interest of the connection to write</li>
	 * </ol>
	 * 
	 * @param event {@link EventData} to be emitted
	 * @return void
	 * @throws EmitException 
	 * @throws IdGenerationFailedException 
	 */
	public static void emit(EventData event) throws EmitException, IdGenerationFailedException
	{
		// 1. Convert the event to the message object
		Message message;
		try 
		{
			message = new EventMessage.Builder()
					.event(event)
					.originatingPort(connector.getPort())
					.build();
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
						.entry("eventType", String.valueOf(event.getEventType()))
						.entry("createdOn", String.valueOf(event.getCreatedOn()))
						.toString())
				.logInfo());

		// 2. Create a ProofOfPublish object and save it to the local DocumentStore. This pod object will be updated when the ack for this message is received from cerebrum.
		ProofOfPublish pod = new ProofOfPublish.Builder()
				.event(event)
				.messageId(message.decoder().getId())
				.createdOn(event.getCreatedOn())
				.producerPortNumber(connector.getPort())
				.producerNodeId(message.decoder().getSourceId())
				.build();
		// 2.1 Set the PUBLISH_EVENT attempts
		pod.incrementEventMessageAttempts();

		try 
		{
			DocumentStore.getInstance().save(pod, message.decoder().getId());
			// MOCK CODE: to test the reliable delivery of the messages
			if(Environment.isDev() && pod.getMessageId().equals("1-12")) 
				return;
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
