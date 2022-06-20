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
 * This class executes within a write worker thread after the channel write operation is done (after sending PUBLISH_EVENT message).<br>
 * Updates following POD attributes:
 * <ol>
 * 	<li> <b>state:</b> set to PUBLISHED only if it is not yet PUBLISHED.</li>
 *  <li> <b>WriteRecord:</b> Time taken (IORecord) to write the PUBLISH_EVENT message on the channel </li>
 * </ol>
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public class PublishedEventAffector extends Affector 
{

	public PublishedEventAffector(Message message, Connection incomingConnection) 
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
		pod.setWriteRecord(getMessage().getWriteRecord());
		// Update pod state only if it is not yet published ( this is done for the case where receptor executes prior to affector )
		if(pod.getPodState().getState() < PodState.PUBLISHED.getState())
			pod.setPodState(PodState.PUBLISHED);
		// Save the POD
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (DocumentSaveFailedException e) {}
	}
}
