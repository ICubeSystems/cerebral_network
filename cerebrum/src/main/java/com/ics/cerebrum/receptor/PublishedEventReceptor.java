package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Date;

import com.ics.cerebrum.message.type.CerebralIncomingMessageType;
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
import com.ics.nceph.core.document.MessageDeliveryState;
import com.ics.nceph.core.document.ProofOfPublish;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.event.exception.EventNotSubscribedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.EventReceptor;

/**
 * This {@link EventReceptor} class is invoked when cerebrum receives a {@link CerebralIncomingMessageType#PUBLISH_EVENT PUBLISH_EVENT} message. <br>
 * The incoming messages is processed as follows:
 * <ol>
 * 	<li>Load {@link ProofOfPublish POD} for the incoming message from the local document store <i>(TBD - query dynamoDB instead of local document store)</i>. </li>
 * 	<li>If POD does not exists then:
 * 		<ol>
 * 			<li>Check if the message was ever received by the connector. If yes then <b>do nothing and return</b>. <i>(This case may arise if the POD has been successfully deleted and for some unknown reasons, the message is resent by the synapse after that.)</i></li>
 * 			<li>Create {@link ProofOfPublish ProofOfPublish} (POD) for the incoming PUBLISH_EVENT message. 
 * 				Set the POD state to {@link MessageDeliveryState#DELIVERED DELIVERED} and save the POD to local document store. 
 * 				The pod is used to track the status of the delivery of the message (on the cerebrum side the POD keeps track of end to end delivery to cerebrum and end to end relay to all the subscribers).</li>
 * 			<li>Update the incoming message {@link Connector#incomingMessageRegister ledger} with this newly received message. <i>(Incoming message ledger contains message ids of all the messages received by the cerebrum)</i></li>
 * 			<li>Enqueue the {@link CerebralOutgoingMessageType#NCEPH_EVENT_ACK NCEPH_EVENT_ACK} message to be sent back to the sender, notifying that the event has been received by cerebrum and the relay to subscriber(s) is in progress.</li>
 * 			<li>Change the type of the message to {@link CerebralOutgoingMessageType#RELAY_EVENT RELAY_EVENT}</li>
 * 			<li>Get the list of subscriber applications (port numbers) and initiate the relay by looping through the list:
 * 				<ol>
 * 					<li>Create {@link ProofOfRelay POR} for every subscriber application</li>
 * 					<li>Get a connection with least load from the subscriber application load balancer ({@link Connector#getConnection()}) </li>
 * 					<li>If no active connection is available on the connector then {@link Connector#enqueueMessage(Message) enqueue} the message on the connector's relay {@link Connector#relayQueue queue} and <code>continue</code> the loop. </li>
 * 					<li>If connection is available then {@link Connection#enqueueMessage(Message, QueuingContext) enqueue} the message on the connection's relay {@link Connection#relayQueue queue} and set the interest of the connection to write</li>
 * 				</ol>
 * 			</li>
 * 		</ol>	
 * 	</li>
 * 	<li>If POD exists then:
 * 		<ol>
 * 			<li>Check if {@link CerebralIncomingMessageType#ACK_RECEIVED ACK_RECEIVED} message is not received then send the {@link CerebralOutgoingMessageType#NCEPH_EVENT_ACK NCEPH_EVENT_ACK} message again</li>
 * 			<li>Else print the duplicate message waring <i>(TBD - send an alert email to developer)</i></li>
 * 		</ol>
 * 	</li>
 * </ol>
 * <br>
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
						.entry("eventType", String.valueOf(getEvent().getEventType()))
						.entry("createdOn", String.valueOf(getEvent().getCreatedOn()))
						.toString())
				.logInfo());
		// 1. Save the message received in the local datastore
		// 1.1 Check if message has already been received
		ProofOfPublish pod = (ProofOfPublish) DocumentStore.load(getMessage().decoder().getId());
		try 
		{
			if (pod == null)
			{
				// TODO: Query the dynamoDB to see if the message was fully delivered previously [TBD]
				// Check if the message was ever received by the connector. This case may arise if the POD has been successfully deleted and for some unknown reason the message is resent by the synapse after that.
				if (getIncomingConnection().getConnector().hasAlreadyReceived(getMessage()))
					return;
				// Create ProofOfPublish object for this message
				pod = new ProofOfPublish.Builder()
						.event(getEvent())
						.messageId(getMessage().decoder().getId())
						.createdOn(getEvent().getCreatedOn())
						.producerPortNumber(getIncomingConnection().getConnector().getPort())
						.producerNodeId(getMessage().decoder().getSourceId())
						.build();
				// 2. Update POD
				// 2.1 Set PUBLISH_EVENT read record
				pod.setEventMessageReadRecord(getMessage().getReadRecord());
				// 2.2 Set PUBLISH_EVENT network record
				pod.setEventMessageNetworkRecord(buildNetworkRecord());
				// 2.4 Set the PUBLISH_EVENT attempts
				pod.incrementEventMessageAttempts();
				// 2.4 Set the acknowledgement attempts
				pod.incrementAcknowledgementMessageAttempts();
				// 2.5 Set POD State to DELIVERED
				pod.setMessageDeliveryState(MessageDeliveryState.DELIVERED);
				// Save the POD in local storage
				DocumentStore.save(pod, getMessage().decoder().getId());

				// Put the message in the connectors incomingMessageStore
				getIncomingConnection().getConnector().storeIncomingMessage(getMessage());
				
				// 3. Send the ACK message (NCEPH_EVENT_ACK) back to the sender notifying that the event has been accepted and the transmission is in progress. 
				sendAcknowledgement(pod);
				
				// 4. BEGIN RELAY: Change the type of the message to RELAY_EVENT
				getMessage().setType(CerebralOutgoingMessageType.RELAY_EVENT.getMessageType());
				
				// 4.1 Get the subscriber connectors for this event
				ArrayList<Connector> subscribers = ConnectorCluster.getSubscribedConnectors(getEvent().getEventType());
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
						por.incrementEventMessageAttempts();
						por.setProducerPortNumber(pod.getProducerPortNumber());
						por.setProducerNodeId(pod.getProducerNodeId());
						por.setConsumerPortNumber(connector.getPort());
						pod.addPor(connector.getPort(), por);

						// Save the POD
						DocumentStore.update(pod, getMessage().decoder().getId());

						// MOCK CODE: to test the reliable delivery of the messages
						if(Environment.isDev() && por.getMessageId().equals("1-11")) 
							continue;
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
							connection.enqueueMessage(getMessage(), QueuingContext.QUEUED_FROM_RECEPTOR);
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
				// If ACK_RECEIVED message is received then send DELETE_POD [DO NOT NEED TO CATER TO THIS - this receptor will never be called if the ACK_RECEIVED message is received]
				if (pod.getMessageDeliveryState().getState() < MessageDeliveryState.ACK_RECIEVED.getState())
					sendAcknowledgement(pod);
				else
					System.out.println("duplicate message found" + getMessage().decoder().getId());
			}
		} 
		catch (DocumentSaveFailedException e){} 
		catch (MessageBuildFailedException e) 
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("NCEPH_EVENT_ACK build failed")
					.logError(),e);
			// decrement acknowledgement attempts in the pod		
			pod.decrementAcknowledgementMessageAttempts();
			// Save the POD
			try 
			{
				DocumentStore.update(pod, getMessage().decoder().getId());
			} 
			catch (DocumentSaveFailedException e1){}
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

	private void sendAcknowledgement(ProofOfPublish pod) throws MessageBuildFailedException
	{
		// 3.1 Create NCEPH_EVENT_ACK message 		
		Message message = new AcknowledgeMessage.Builder()
				.data(
						new AcknowledgementData.Builder()
						.readRecord(getMessage().getReadRecord())
						.eventNetworkRecord(pod.getEventMessageNetworkRecord())
						.build())
				.messageId(getMessage().getMessageId())
				.type(CerebralOutgoingMessageType.NCEPH_EVENT_ACK.getMessageType())
				.sourceId(getMessage().getSourceId())
				.build();
		// 3.2 Enqueue NCEPH_EVENT_ACK for sending
		getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
		getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
	}
}
