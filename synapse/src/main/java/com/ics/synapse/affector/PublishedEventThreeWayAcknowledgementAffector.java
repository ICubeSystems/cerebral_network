package com.ics.synapse.affector;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PodState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.Message;

/**
 * This class executes within a write worker thread after the channel write operation is done (after sending ACK_RECEIVED message).<br>
 * Updates following POD attributes:
 * <ol>
 * 	<li> <b>state:</b> set to ACK_RECIEVED only if it is not yet ACK_RECIEVED.</li>
 *  <li> <b>ThreeWayAckWriteRecord:</b> Time taken (IORecord) to write the ACK_RECEIVED message on the channel </li>
 * </ol> 
 * @author Anshul
 * @version 1.0
 * @since 30-03-2022
 *
 */
public class PublishedEventThreeWayAcknowledgementAffector extends Affector 
{

	public PublishedEventThreeWayAcknowledgementAffector(Message message, Connection incomingConnection) 
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
		pod.setThreeWayAckWriteRecord(getMessage().getWriteRecord());
		// Update pod state only if it is not yet acknowledged ( this is done for the case where receptor executes prior to affector )
		if(pod.getPodState().getState() < PodState.ACK_RECIEVED.getState())
			pod.setPodState(PodState.ACK_RECIEVED);
		// Save the POD
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (DocumentSaveFailedException e) {}
	}
}