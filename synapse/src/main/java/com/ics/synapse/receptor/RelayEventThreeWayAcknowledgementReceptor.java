package com.ics.synapse.receptor;

import java.nio.channels.SelectionKey;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementDoneData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.ThreeWayAcknowledgementReceptor;
import com.ics.synapse.applicationReceptor.ApplicationReceptor;
import com.ics.synapse.applicationReceptor.exception.ApplicationReceptorFailedException;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;
import com.ics.synapse.ncephEvent.Event;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 10-Apr-2022
 */
public class RelayEventThreeWayAcknowledgementReceptor extends ThreeWayAcknowledgementReceptor 
{
	public RelayEventThreeWayAcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{                                                                                                                                                                                                                                                                                                                                                                                                                                                                   
		// 1. Save the write record and three way acknowledgement record in the local datastore
		ProofOfRelay por = ProofOfRelay.load(getMessage().decoder().getOriginatingPort(), getIncomingConnection().getConnector().getPort(), getMessage().decoder().getId());
		if (por == null)
		{
			// TODO: Handle case a.
			// Log the fatal error if the POR is not in the local storage
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POR not found")
					.logInfo());
			return;
		}
		try 
		{
			// 2. Create a NetworkRecord for 3-way relay ack and update the POR
			// 2.1 set RELAY_EVENT WriteRecord which is sent in RELAY_ACK_RECEIVED message body
			por.setEventMessageWriteRecord(getThreeWayAcknowledgement().getWriteRecord());
			// 2.2 set RELAY_ACK_RECEIVED network record
			por.setThreeWayAckMessageNetworkRecord(buildNetworkRecord());
			// 2.3 set RELAYED_EVENT_ACK network record which is sent in RELAY_ACK_RECEIVED message body
			por.setAckMessageNetworkRecord(getThreeWayAcknowledgement().getAckNetworkRecord());
			// 2.4 set RELAY_ACK_RECEIVED read record
			por.setThreeWayAckMessageReadRecord(getMessage().getReadRecord());
			// 2.5 Set the threeWayAck attempts
			por.incrementThreeWayAckMessageAttempts();
			// 2.6 Set the delePod attempts
			por.incrementFinalMessageAttempts();
			// 2.7 Set POR State to ACK_RECIEVED
			por.setMessageDeliveryState(MessageDeliveryState.ACK_RECIEVED.getState());
			// 2.5 Update the POD in the local storage
			DocumentStore.getInstance().update(por, getMessage().decoder().getId());
			
			// Application receptor execution failed to execute from RelayedEventReceptor, following 2 cases arise:
			// CASE 1: this class is called via 3way ack message from cerebrum 
			// CASE 2: this class is called due to cerebral monitor resending the 3way ack message
			if(por.isAppReceptorFailed()) 
				initiateApplicationReceptor(por); //execute the application receptor again

			// If the application receptor has been executed successfully then send the POR_DELETED message back to cerebrum.
			if(!por.isAppReceptorFailed()) 
			{
				// 3.0 Create the message data for POR_DELETED message to be sent to cerebrum
				AcknowledgementDoneData deletePor = new AcknowledgementDoneData.Builder()
						.threeWayAckNetworkRecord(buildNetworkRecord())
						.build();
				// 3.1 Create the POR_DELETED message 
				Message message = new AcknowledgeMessage.Builder()
						.data(deletePor)
						.messageId(getMessage().getMessageId())
						.type(SynapticOutgoingMessageType.POR_DELETED.getMessageType())
						.sourceId(getMessage().getSourceId())
						.originatingPort(getMessage().getOriginatingPort())
						.build();
				// 3.2 Enqueue POD_DELETED on the incoming connection
				getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
				getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
			}
		} 
		catch (DocumentSaveFailedException e) {}
		catch (MessageBuildFailedException e1) {
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("POR_DELETED build failed")
					.logError(),e1);
			// decrement acknowledgement attempts in the pod		
			por.decrementFinalMessageAttempts();
			// Save the POD
			try 
			{
				DocumentStore.getInstance().update(por, getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e){}
			return;
		}
	}
	
	private void initiateApplicationReceptor(ProofOfRelay por) throws DocumentSaveFailedException 
	{
		// Invoke appropriate ApplicationReceptor
		try 
		{
			por.incrementAppReceptorExecutionAttempts();
			ApplicationReceptor<? extends Event> applicationReceptor = new ApplicationReceptor.Builder<>()
					.eventData(por.getEvent())
					.build();
			// Execute the application receptor class and record the execution time
			long startTime = System.currentTimeMillis();
			applicationReceptor.execute();
			por.setAppReceptorFailed(false);
			por.setAppReceptorExecutionTime((System.currentTimeMillis() - startTime));
		} catch (ApplicationReceptorFailedException e) 
		{
			por.setAppReceptorExecutionErrorMsg(e.getMessage());
			por.setAppReceptorFailed(true);
			// LOG
		}
		DocumentStore.getInstance().update(por, getMessage().decoder().getId());
	}
}
