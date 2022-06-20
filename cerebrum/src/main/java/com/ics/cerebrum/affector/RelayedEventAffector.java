package com.ics.cerebrum.affector;

import java.io.IOException;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PorState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.ProofOfRelay;
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
		// Load the POD for this message
		ProofOfDelivery pod = (ProofOfDelivery)DocumentStore.load(getMessage().decoder().getId());
		if (pod == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}
		
		// Set the WriteRecord in the POR
		ProofOfRelay por = pod.getPors().get(getIncomingConnection().getConnector().getPort());
		por.setWriteRecord(getMessage().getWriteRecord());
		
		// Update POR state only if it is not yet RELAYED (this is done for the case where receptor executes prior to affector)
		if(por.getPorState().getState() < PorState.RELAYED.getState())
			por.setPorState(PorState.RELAYED);
		
		// Save the POD
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (IOException e) {}
	}
}
