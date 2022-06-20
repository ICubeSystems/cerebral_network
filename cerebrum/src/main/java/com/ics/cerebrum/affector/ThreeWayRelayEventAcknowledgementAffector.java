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
public class ThreeWayRelayEventAcknowledgementAffector extends Affector 
{

	public ThreeWayRelayEventAcknowledgementAffector(Message message, Connection incomingConnection) 
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
		
		// Set the WriteRecord in the POD
		ProofOfRelay por = pod.getPors().get(getIncomingConnection().getConnector().getPort());
		por.setThreeWayAckWriteRecord(getMessage().getWriteRecord());
		
		// Update por state only if it is not yet ACK_RECIEVED (this is done for the case where receptor executes prior to affector)
		if(por.getPorState().getState() < PorState.ACK_RECIEVED.getState())
			por.setPorState(PorState.ACK_RECIEVED);
		
		// Save the POD
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (IOException e) {}
	}
}
