package com.ics.synapse.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Date;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.event.Acknowledgement;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
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
			
			por = new ProofOfRelay.Builder()
					.event(getEvent())
					.messageId(getMessage().decoder().getId())
					.relayedOn(new Date())
					.build();
			por.setReadRecord(getMessage().getReadRecord());
			
			// Save the POD in local storage
			try {
				DocumentStore.save(por, ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
			} catch (IOException e1) {}
		
			// 2. Send the ACK message (RELAYED_EVENT_ACK) back to the sender notifying that the event has been accepted and the transmission is in progress. 
			try 
			{
				NetworkRecord networkRecord = new NetworkRecord.Builder().start(new Date()).build();
				por.setAckNetworkRecord(networkRecord);
				// 2.1 Create NCEPH_EVENT_ACK message 
				Message message = new AcknowledgeMessage.Builder()
						.data(new Acknowledgement.Builder()
								.readRecord(getMessage().getReadRecord())
								.ackNetworkRecord(networkRecord)
								.build())
						.messageId(getMessage().getMessageId())
						.type(SynapticOutgoingMessageType.RELAYED_EVENT_ACk.getMessageType())
						.sourceId(getMessage().getSourceId())
						.build();
				
				// 2.2 Enqueue RELAYED_EVENT_ACK for sending
				getIncomingConnection().enqueueMessage(message);
				getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
			} catch (IOException e) 
			{
				NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
						.messageId(getMessage().decoder().getId())
						.action("RELAYED_EVENT_ACK build error")
						.logError(),e);
			}
		}
		else
		{
			// duplicate message handling - TBD
		}
	}
}
