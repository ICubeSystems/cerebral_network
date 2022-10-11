package com.ics.cerebrum.affector;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.Message;

/**
 * This class executes within a write worker thread after the channel write operation is done (after sending RELAY_ACK_RECEIVED message).<br>
 * Updates following POR attributes:
 * <ol>
 * 	<li> <b>state:</b> set to ACK_RECIEVED only if it is not yet ACK_RECIEVED.</li>
 *  <li> <b>threeWayAckWriteRecord:</b> Time taken (IORecord) to write the RELAY_ACK_RECEIVED message on the channel </li>
 * </ol>
 * @author Anshul
 * @version 1.0
 * @since 10-Apr-2022
 */
public class RelayEventThreeWayAcknowledgementAffector extends Affector 
{

	public RelayEventThreeWayAcknowledgementAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// Load the POR for this message
		ProofOfRelay por = ProofOfRelay.load(getMessage().decoder().getOriginatingPort(), getIncomingConnection().getConnector().getPort(), getMessage().decoder().getId());
		if (por == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POR not found")
					.logInfo());
			return;
		}
		
		// Set the WriteRecord in the POD
		por.setThreeWayAckMessageWriteRecord(getMessage().getWriteRecord());
		
		// Update por state only if it is not yet ACK_RECIEVED (this is done for the case where receptor executes prior to affector)
		if(por.getMessageDeliveryState() < MessageDeliveryState.ACK_RECIEVED.getState())
			por.setMessageDeliveryState(MessageDeliveryState.ACK_RECIEVED.getState());
		
		// Save the POD
		try {
			DocumentStore.getInstance().update(por, getMessage().decoder().getId());
		} catch (DocumentSaveFailedException e) {}
	}
}
