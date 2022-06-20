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
 * This class executes within a write worker thread after the channel write operation is done (After sending DELETE_POD message). POD state is set to Finished.
 * Updates following POD attributes:
 * <ol>
 * 	<li> <b>state:</b> set to FINISHED. </li>
 * </ol>  
 * @author Anshul
 * @version 1.0
 * * @since 30-Mar-2022
 */
public class DeletePodAffector extends Affector 
{

	public DeletePodAffector(Message message, Connection incomingConnection) 
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
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}
		pod.setPodState(PodState.FINISHED);
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (IOException e) {}
	}
}
