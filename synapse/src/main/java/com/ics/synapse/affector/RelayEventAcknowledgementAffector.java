package com.ics.synapse.affector;

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
 * This class executes within a write worker thread after the channel write operation is done (after sending RELAYED_EVENT_ACK message).<br>
 * Updates following POR attributes:
 * <ol>
 * 	<li> <b>state:</b> set to ACKNOWLEDGED only if it is not yet ACKNOWLEDGED.</li>
 *  <li> <b>AckWriteRecord:</b> Time taken (IORecord) to write the RELAYED_EVENT_ACK message on the channel </li>
 * </ol> 
 * @author Anshul
 * @version 1.0
 * * @since 10-Apr-2022
 */
public class RelayEventAcknowledgementAffector extends Affector 
{

	public RelayEventAcknowledgementAffector(Message message, Connection incomingConnection) 
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
		
		por.setAckMessageWriteRecord(getMessage().getWriteRecord());
		// Update por state only if it is not yet ACKNOWLEDGED ( this is done for the case where receptor executes prior to affector )
		if(por.getMessageDeliveryState() < MessageDeliveryState.ACKNOWLEDGED.getState())
			por.setMessageDeliveryState(MessageDeliveryState.ACKNOWLEDGED.getState());
		// Save the POD
		try {
			DocumentStore.getInstance().update(por, getMessage().decoder().getId());
		} catch (DocumentSaveFailedException e) {}
	}
}
