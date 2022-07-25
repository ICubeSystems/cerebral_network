package com.ics.synapse.receptor;

import java.nio.channels.SelectionKey;
import java.util.Date;

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
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.message.data.AcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.EventReceptor;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 10-Apr-2022
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
		if (por == null) // If the ProofOfRelay for the received message is not in the local storage then create a new ProofOfRelay object for this message
		{
			// Build ProofOfRelay object for this message
			try 
			{
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
				// Save the POR in local storage

				DocumentStore.save(por, ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());

				// Invoke appropriate ApplicationReceptor
				
				
				// 2. Send the ACK message (RELAYED_EVENT_ACK) back to the sender notifying that the event has been accepted and the transmission is in progress. 
				NetworkRecord networkRecord = new NetworkRecord.Builder().start(new Date().getTime()).build();
				por.setAckNetworkRecord(networkRecord);
				// 2.1 Create NCEPH_EVENT_ACK message 
				Message message = new AcknowledgeMessage.Builder()
						.data(new AcknowledgementData.Builder()
								.readRecord(getMessage().getReadRecord())
								.ackNetworkRecord(networkRecord)
								.eventNetworkRecord(por.getEventNetworkRecord())
								.build())
						.messageId(getMessage().getMessageId())
						.type(SynapticOutgoingMessageType.RELAYED_EVENT_ACK.getMessageType())
						.sourceId(getMessage().getSourceId())
						.build();

				// 2.2 Enqueue RELAYED_EVENT_ACK for sending
				getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
				getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
			} 
			catch (DocumentSaveFailedException e) {}
			catch (MessageBuildFailedException e1) {
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
		else
		{
			System.out.println("duplicate found " + getMessage().decoder().getId());
			// duplicate message handling - TBD
		}
	}
}
