package com.ics.synapse.receptor;

import java.nio.channels.SelectionKey;
import java.util.Date;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.config.ConfigStore;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PorState;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.EventReceptor;
import com.ics.synapse.applicationReceptor.ApplicationReceptor;
import com.ics.synapse.applicationReceptor.exception.ApplicationReceptorFailedException;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * Receptor class to receive event data relayed by cerebrum, which have following attributes:
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
		ProofOfRelay por =  (ProofOfRelay) DocumentStore.load(ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
		try 
		{
			if (por == null) // If the ProofOfRelay for the received message is not in the local storage then create a new ProofOfRelay object for this message
			{
				// Build ProofOfRelay object for this message
				por = new ProofOfRelay.Builder()
						.event(getEvent())
						.messageId(getMessage().decoder().getId())
						.relayedOn(new Date().getTime())
						.build();
				
				// 2. Update POR
				// 2.1 Set RELAY_EVENT read record
				por.setReadRecord(getMessage().getReadRecord());
				// 2.2 Set RELAY_EVENT network record
				por.setEventNetworkRecord(buildNetworkRecord());
				// 2.4 Set the RELAY_EVENT attempts
				por.incrementRelayAttempts();
				// 2.4 Set the acknowledgement attempts
				por.incrementAcknowledgementAttempts();
				// 2.5 Set POR State to RELAYED
				por.setPorState(PorState.RELAYED);
				// 2.6 Save the POR in local storage
				DocumentStore.save(por, ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
				
				//3. Initiate application receptor 
				//In case of error in execute(), we do not break the flow. Instead the POR is updated and the execution data is send to cerebrum in the acknowledgement message.
				initiateApplicationReceptor(por);
				
				// 4. send acknowledgement back to cerebrum
				sendAcknowledgement(por);
			}
			else
			{
				// send acknowledgement back to cerebrum if the POR state is still RELAYED or ACKNOWLEDGED
				if (por.getPorState().getState() < PorState.ACK_RECIEVED.getState())
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
		catch (DocumentSaveFailedException e) {}
		catch (MessageBuildFailedException e1) 
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("RELAYED_EVENT_ACK build failed")
					.logError(),e1);
			// decrement acknowledgement attempts in the POR		
			por.decrementAcknowledgementAttempts();
			// Save the POD
			try 
			{
				DocumentStore.update(por, ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e){}
			return;
		}
	}
	
	private void initiateApplicationReceptor(ProofOfRelay por) throws DocumentSaveFailedException 
	{
		try 
		{
			por.incrementAppReceptorExecutionAttempts();
			por.setAppReceptorName(ConfigStore.getApplicationReceptor(getEvent().getEventType()));
			// Invoke appropriate ApplicationReceptor
			ApplicationReceptor applicationReceptor = new ApplicationReceptor.Builder()
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
		DocumentStore.save(por, ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
	}
	
	private void sendAcknowledgement(ProofOfRelay por) throws MessageBuildFailedException 
	{
		// Mock code
		if(por.getMessageId().equals("1-3") ) {
			return;
		}
		// 4.1 Create NCEPH_EVENT_ACK message 
		Message message = new AcknowledgeMessage.Builder()
				.data(new AcknowledgementData.Builder()
						.readRecord(getMessage().getReadRecord())
						.eventNetworkRecord(por.getEventNetworkRecord())
						.AppReceptorExecutionErrorMsg(por.getAppReceptorExecutionErrorMsg())
						.AppReceptorName(por.getAppReceptorName())
						.AppReceptorExecutionTime(por.getAppReceptorExecutionTime())
						.appReceptorFailed(por.isAppReceptorFailed())
						.build())
				.messageId(getMessage().getMessageId())
				.type(SynapticOutgoingMessageType.RELAYED_EVENT_ACK.getMessageType())
				.sourceId(getMessage().getSourceId())
				.build();
		
		// 2.2 Enqueue RELAYED_EVENT_ACK for sending
		getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
		getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
	}
}
