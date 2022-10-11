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
 * This class executes within a write worker thread after the channel write operation is done (After sending RELAY_EVENT message).<br>
 * Updates following POR attributes:
 * <ol>
 * 	<li> <b>state:</b> set to RELAYED only if it is not yet RELAYED.</li>
 *  <li> <b>WriteRecord:</b> Time taken (IORecord) to write the RELAYED message on the channel </li>
 * </ol>
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public class RelayedEventAffector extends Affector 
{

	public RelayedEventAffector(Message message, Connection incomingConnection) 
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

		// Set the WriteRecord in the POR
		por.setEventMessageWriteRecord(getMessage().getWriteRecord());

		// Update POR state only if it is not yet RELAYED (this is done for the case where receptor executes prior to affector)
		if(por.getMessageDeliveryState() < MessageDeliveryState.DELIVERED.getState())
			por.setMessageDeliveryState(MessageDeliveryState.DELIVERED.getState());

		// Save the POD
		try {
			DocumentStore.getInstance().update(por, getMessage().decoder().getId());
		} catch (DocumentSaveFailedException e) {}
	}
}
