package com.ics.cerebrum.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Date;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.event.AcknowledgementDone;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.receptor.ThreeWayAcknowledgementReceptor;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 30-Mar-2022
 */
public class ThreeWayEventAcknowledgementReceptor extends ThreeWayAcknowledgementReceptor 
{
	public ThreeWayEventAcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Save the write record and three way acknowledgement record in the local datastore
		ProofOfDelivery pod = (ProofOfDelivery) DocumentStore.load(getMessage().decoder().getId());
		// Following are the cases when the POD will not be found for the message id:
		// a. POD is uploaded to the DB and 3-way ack is received after that [In this case we should check the DB and send Delete message if POD exists in DB]
		// b. POD is deleted by mistake on the synaptic node [Handling TBD - for now just logging such occurrence]
		if (pod == null)
		{
			// TODO: Handle case a.
			// Log the fatal error if the POD is not in the local storage and not in DB
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}
		
		// 2. Create a NetworkRecord for 3-way ack and update the POD
		NetworkRecord networkRecord = new NetworkRecord.Builder()
											.start(getThreeWayAcknowledgement().getThreeWayAckNetworkRecord().getStart())
											.end(new Date()).build();
		// 2.1 set ThreeWayAckReadRecord
		pod.setThreeWayAckReadRecord(getMessage().getReadRecord());
		// 2.2 set PUBLISH_EVENT WriteRecord which is sent in ACK_RECEIVED message body
		pod.setWriteRecord(getThreeWayAcknowledgement().getWriteRecord());
		// 2.3 set ACK_RECEIVED network record
		pod.setThreeWayAckNetworkRecord(networkRecord);
		// 2.4 set NCEPH_EVENT_ACK network record which is sent in ACK_RECEIVED message body
		pod.setAckNetworkRecord(getThreeWayAcknowledgement().getAckNetworkRecord());
		// 2.5 Update the POD in the local storage
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (IOException e1) {}
		
		try 
		{
			// 3.0 Create the message data for DELETE_POD message to be sent to synapse
			AcknowledgementDone deletePod = new AcknowledgementDone.Builder()
					.threeWayAckNetworkRecord(pod.getThreeWayAckNetworkRecord())
					.build();
			// 3.1 Create the DELETE_POD message 			
			Message message = new AcknowledgeMessage.Builder()
					.data(deletePod)
					.messageId(getMessage().getMessageId())
					.type(CerebralOutgoingMessageType.DELETE_POD.getMessageType())
					.sourceId(getMessage().getSourceId())
					.build();
			// 3.2 Enqueue DELETE_POD on the incoming connection
			getIncomingConnection().enqueueMessage(message);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		} catch (IOException e) 
		{
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("DELETE_POD build error")
					.logError(),e);
		}
	}
}
