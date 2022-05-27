package com.ics.synapse.receptor;

import java.io.IOException;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.receptor.PodReceptor;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 30-Mar-2022
 */
public class DeletePodReceptor extends PodReceptor 
{
	public DeletePodReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load the POD to delete
		ProofOfDelivery pod = (ProofOfDelivery)DocumentStore.load(getMessage().decoder().getId());
		if (pod == null)
		{
			// Log and return
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}
		
		// 2. Set ThreeWayAckNetworkRecord and save to local storage. Save is only required in case the delete operation fails
		pod.setThreeWayAckNetworkRecord(getPod().getThreeWayAckNetworkRecord());
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (IOException e) {}
		
		// 3. Delete the POD from local storage
		if (!DocumentStore.delete(getMessage().decoder().getId(),pod))
		{
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("POD deletion failed")
					.logError());
			return;
		}
	}
}
