package com.ics.synapse.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Date;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.event.AcknowledgementDone;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
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
		ProofOfRelay por =  (ProofOfRelay) DocumentStore.load("p"+getMessage().decoder().getId());
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
		
		// 2. Create a NetworkRecord for 3-way relay ack and update the POR
		NetworkRecord networkRecord = new NetworkRecord.Builder()
										.start(getThreeWayAcknowledgement().getThreeWayAckNetworkRecord().getStart())
										.end(new Date()).build();
		// 2.1 set RELAY_EVENT WriteRecord which is sent in RELAY_ACK_RECEIVED message body
		por.setWriteRecord(getThreeWayAcknowledgement().getWriteRecord());
		// 2.2 set RELAY_ACK_RECEIVED network record
		por.setThreeWayAckNetworkRecord(networkRecord);
		// 2.3 set RELAYED_EVENT_ACK network record which is sent in RELAY_ACK_RECEIVED message body
		por.setAckNetworkRecord(getThreeWayAcknowledgement().getAckNetworkRecord());
		// 2.4 set RELAY_ACK_RECEIVED read record
		por.setThreeWayAckReadRecord(getMessage().getReadRecord());
		// 2.5 Update the POD in the local storage
		try {
			DocumentStore.save(por, "p"+getMessage().decoder().getId());
		} catch (IOException e) {}
		
		
		// Delete the POR
		if (!DocumentStore.delete("p"+getMessage().decoder().getId(),por))
		{
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("POR deletion failed")
					.logError());
			return;
		}
		
		try 
		{
			// 3.0 Create the message data for POR_DELETED message to be sent to cerebrum
			AcknowledgementDone deletePor = new AcknowledgementDone.Builder()
					.threeWayAckNetworkRecord(por.getThreeWayAckNetworkRecord())
					.build();
			// 3.1 Create the POR_DELETED message 
			Message message = new AcknowledgeMessage.Builder()
					.data(deletePor)
					.messageId(getMessage().getMessageId())
					.type(SynapticOutgoingMessageType.POR_DELETED.getMessageType())
					.sourceId(getMessage().getSourceId())
					.build();
			// 3.2 Enqueue POD_DELETED on the incoming connection
			getIncomingConnection().enqueueMessage(message);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		} catch (IOException e) {
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("POR_DELETED build failed")
					.logError(),e);
		}
	}
}
