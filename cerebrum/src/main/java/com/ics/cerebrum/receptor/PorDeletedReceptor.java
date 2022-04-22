package com.ics.cerebrum.receptor;

import java.io.IOException;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.receptor.PodReceptor;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 10-Apr-2022
 */
public class PorDeletedReceptor extends PodReceptor 
{
	public PorDeletedReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load the POD to Update RELAY_ACK_RECEIVED network record.
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
		
		ProofOfRelay por = pod.getPors().get(getIncomingConnection().getConnector().getPort());
		// 2. Set ThreeWayRelayAckNetworkRecord and save to local storage.
		por.setThreeWayAckNetworkRecord(getPod().getThreeWayAckNetworkRecord());
		
		try {
			DocumentStore.save(pod, getMessage().decoder().getId());
		} catch (IOException e) {}
	}
}
