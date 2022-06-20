package com.ics.cerebrum.affector;

import java.io.IOException;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PodState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.message.Message;

/**
 * This class executes within a write worker thread after the channel write operation is done (after sending NCEPH_EVENT_ACK message).<br>
 * Updates following POD attributes:
 * <ol>
 * 	<li> <b>state:</b> set to ACKNOWLEDGED only if it is not yet ACKNOWLEDGED.</li>
 *  <li> <b>AckWriteRecord:</b> Time taken (IORecord) to write the NCEPH_EVENT_ACK message on the channel </li>
 * </ol> 
 * @author Anshul
 * @version 1.0
 * * @since 30-Mar-2022
 */
public class EventAcknowledgementAffector extends Affector 
{

	public EventAcknowledgementAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// Load the POD for this message
		ProofOfDelivery pod = (ProofOfDelivery)DocumentStore.load(getMessage().decoder().getId());
		// LOG: In case POD is not loaded from the cache
		if (pod == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}

		pod.setAckWriteRecord(getMessage().getWriteRecord());
		// Update pod state only if it is not yet ACKNOWLEDGED (this is done for the case where receptor executes prior to affector)
		if(pod.getPodState().getState() < PodState.ACKNOWLEDGED.getState())
			pod.setPodState(PodState.ACKNOWLEDGED);
		// Save the POD
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (IOException e) {}
	}
}
