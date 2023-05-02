
package com.ics.synapse.receptor;

import java.nio.channels.SelectionKey;
import java.util.Date;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.ConfigStore;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.EventReceptor;
import com.ics.synapse.applicationReceptor.ApplicationReceptor;
import com.ics.synapse.applicationReceptor.exception.ApplicationReceptorFailedException;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;
import com.ics.synapse.ncephEvent.Event;

/**
 * Receptor class to receive event relayed by cerebrum, which have following attributes:
 * 	<ol>
 * 		<li>eventId</li>
 * 		<li>eventType</li>
 * 		<li>objectJSON</li>
 * 		<li>createdOn</li>
 * 	</ol>
 * After receiving event data, it creates a {@link ProofOfRelay POR} and save event data in local store.<br>
 * 
 * @author Anshul
 * @version 1.0
 * @since 10-Apr-2022
 */
public class RelayedEventReceptor extends EventReceptor 
{
	public RelayedEventReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Save the event received in the local datastore
		// 1.1 Check if message has already been received
		ProofOfRelay por = ProofOfRelay.load(getMessage().decoder().getOriginatingPort(), getIncomingConnection().getConnector().getPort(), getMessage().decoder().getId());
		try 
		{
			if (por == null) // If the ProofOfRelay for the received message is not in the local storage then create a new ProofOfRelay object for this message
			{
				// TODO: Query the dynamoDB to see if the message was fully delivered previously [TBD]
				// Check if the message was ever received by the connector. This case may arise if the POD has been successfully deleted and for some unknown reason the message is resent by the synapse after that.
				if (getIncomingConnection().getConnector().hasAlreadyReceived(getMessage()))
					return;
				
				// Build ProofOfRelay object for this message
				por = new ProofOfRelay.Builder()
						.event(getEvent())
						.messageId(getMessage().decoder().getId())
						.relayedOn(new Date().getTime())
						.consumerPort(getIncomingConnection().getConnector().getPort())
						.producerPort(getMessage().decoder().getOriginatingPort())
						.producerNodeId(getMessage().decoder().getSourceId())
						.build();
				
				// 2. Update POR
				// 2.1 Set RELAY_EVENT read record
				por.setEventMessageReadRecord(getMessage().getReadRecord());
				// 2.2 Set RELAY_EVENT network record
				por.setEventMessageNetworkRecord(buildNetworkRecord());
				// 2.3 Set the RELAY_EVENT attempts
				por.incrementEventMessageAttempts();
				// 2.4 Set the consumerNodeId
				por.setConsumerNodeId(ConfigStore.getInstance().getNodeId());
				// 2.5 increment AcknowledgementMessageAttempts 
				por.incrementAcknowledgementMessageAttempts();
				// 2.6 Set MessageDeliveryState State to RELAYED
				por.setMessageDeliveryState(MessageDeliveryState.DELIVERED.getState());
				// 2.7 Put the message in the connectors incomingMessageStore
				getIncomingConnection().getConnector().storeIncomingMessage(getMessage());
				
				//CRASH HANDLING: If synapse terminates/ crashes before this point then relay monitor in cerebrum will re-transmit the relay message and it will execute the if as the POR has not been saved as of yet
				
				// 2.8 Save the POR in local storage
				DocumentStore.getInstance().save(por, getMessage().decoder().getId());
				
				//CRASH HANDLING: If synapse terminates/ crashes after this point then Execution of the application receptor will handled by relay monitor, cerebrum will re transmit relay message (it will execute in the else below as the POR will exist in the synapse)
				 
				//3. Initiate application receptor 
				//In case of error in execute(), we do not break the flow. Instead the POR is updated and the execution data is send to cerebrum in the acknowledgement message.
				initiateApplicationReceptor(por);
				//CRASH HANDLING: If synapse terminates/ crashes after this point then ACK message (RELAYED_EVENT_ACK) back to the sender will be sent when relay monitor in cerebrum will re transmit the relay message (it will execute in the else below as the POR will exist in the synapse)
				
				// 4. send acknowledgement back to cerebrum
				sendAcknowledgement(por);
			}
			else
			{
				// send acknowledgement back to cerebrum if the POR state is still RELAYED or ACKNOWLEDGED
				if (por.getMessageDeliveryState() < MessageDeliveryState.ACK_RECIEVED.getState())
				{
					//Initiate application receptor if its execution was failed
					if(por.isAppReceptorFailed()) 
						initiateApplicationReceptor(por);
					sendAcknowledgement(por);
				}
				else
					System.out.println("duplicate found " + getMessage().decoder().getId());
			}
		}
		catch (DocumentSaveFailedException | MessageBuildFailedException e) {
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("RELAYED_EVENT_ACK build failed")
					.description("Due to: " + e.getMessage())
					.logError(),e);
			// remove the message in the connector's incomingMessageStore
			getIncomingConnection().getConnector().removeIncomingMessage(getMessage());
			// decrement acknowledgement attempts in the POR		
			por.decrementAcknowledgementMessageAttempts();
			
			// Save the POD
			try 
			{
				DocumentStore.getInstance().save(por, getMessage().decoder().getId());//TODO - check if this should be save instead of update
			} catch (DocumentSaveFailedException e1){}
			return;
		}
	}
	
	private void initiateApplicationReceptor(ProofOfRelay por) throws DocumentSaveFailedException 
	{
		try 
		{
			por.incrementAppReceptorExecutionAttempts();
			por.setAppReceptorName(ConfigStore.getInstance().getApplicationReceptor(getEvent().getEventType()));
			// Invoke appropriate ApplicationReceptor
			ApplicationReceptor<? extends Event> applicationReceptor = new ApplicationReceptor.Builder<>()
					.eventData(por.getEvent())
					.build();
			// Execute the application receptor class and record the execution time
			long startTime = System.currentTimeMillis();
			// Execute
			applicationReceptor.execute();
			// Record the execution time in POR
			por.setAppReceptorExecutionTime((System.currentTimeMillis() - startTime));
			por.setAppReceptorFailed(false);
		} catch (ApplicationReceptorFailedException e) 
		{
			por.setAppReceptorExecutionErrorMsg(e.getMessage());
			por.setAppReceptorFailed(true);
			// LOG
		}
		DocumentStore.getInstance().update(por, getMessage().decoder().getId());
	}
	
	private void sendAcknowledgement(ProofOfRelay por) throws MessageBuildFailedException 
	{
		// 4.1 Create NCEPH_EVENT_ACK message 
		Message message = new AcknowledgeMessage.Builder()
				.data(new AcknowledgementData.Builder()
						.readRecord(getMessage().getReadRecord())
						.eventNetworkRecord(por.getEventMessageNetworkRecord())
						.AppReceptorExecutionErrorMsg(por.getAppReceptorExecutionErrorMsg())
						.AppReceptorName(por.getAppReceptorName())
						.AppReceptorExecutionTime(por.getAppReceptorExecutionTime())
						.appReceptorFailed(por.isAppReceptorFailed())
						.nodeId(Integer.valueOf(ConfigStore.getInstance().getNodeId()))
						.build())
				.messageId(getMessage().getMessageId())
				.type(SynapticOutgoingMessageType.RELAYED_EVENT_ACK.getMessageType())
				.sourceId(getMessage().getSourceId())
				.originatingPort(getMessage().getOriginatingPort())
				.build();
		
		// 2.2 Enqueue RELAYED_EVENT_ACK for sending
		getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
		getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
	}
}
