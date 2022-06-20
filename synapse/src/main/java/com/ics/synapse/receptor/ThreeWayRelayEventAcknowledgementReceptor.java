package com.ics.synapse.receptor;

import java.nio.channels.SelectionKey;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PorState;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementDoneData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.ThreeWayAcknowledgementReceptor;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 10-Apr-2022
 */
public class ThreeWayRelayEventAcknowledgementReceptor extends ThreeWayAcknowledgementReceptor 
{
	public ThreeWayRelayEventAcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		
		// 1. Save the write record and three way acknowledgement record in the local datastore
		ProofOfRelay por =  (ProofOfRelay) DocumentStore.load(ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
		if (por == null)
		{
			// TODO: Handle case a.
			// Log the fatal error if the POR is not in the local storage
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}
		try {

			// 2. Create a NetworkRecord for 3-way relay ack and update the POR
			// 2.1 set RELAY_EVENT WriteRecord which is sent in RELAY_ACK_RECEIVED message body
			por.setWriteRecord(getThreeWayAcknowledgement().getWriteRecord());
			// 2.2 set RELAY_ACK_RECEIVED network record
			por.setThreeWayAckNetworkRecord(buildNetworkRecord());
			// 2.3 set RELAYED_EVENT_ACK network record which is sent in RELAY_ACK_RECEIVED message body
			por.setAckNetworkRecord(getThreeWayAcknowledgement().getAckNetworkRecord());
			// 2.4 set RELAY_ACK_RECEIVED read record
			por.setThreeWayAckReadRecord(getMessage().getReadRecord());
			// 2.5 Set the threeWayAck attempts
			por.incrementThreeWayAckAttempts();
			// 2.6 Set the delePod attempts
			por.incrementDeletePorAttempts();
			// 2.7 Set POR State to ACK_RECIEVED
			por.setPorState(PorState.ACK_RECIEVED);
			// 2.5 Update the POD in the local storage

			DocumentStore.update(por,  ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
			
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
					.build();
			// 3.2 Enqueue POD_DELETED on the incoming connection
			getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
			// Delete the POR
			por.setPorState(PorState.FINISHED);
			if (!DocumentStore.delete(ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId(),por))
			{
				NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
						.messageId(getMessage().decoder().getId())
						.action("POR deletion failed")
						.logError());
				return;
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
			por.decrementDeletePorAttempts();
			// Save the POD
			try 
			{
				DocumentStore.update(por, ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e){}
			return;
		}
	}
}
