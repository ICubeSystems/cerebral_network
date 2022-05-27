package com.ics.synapse.affector;

import java.io.IOException;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 30-03-2022
 *
 */
public class ThreeWayEventAcknowledgementAffector extends Affector 
{

	public ThreeWayEventAcknowledgementAffector(Message message, Connection incomingConnection) 
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
		// Save the POD
		try {
			DocumentStore.update(pod, getMessage().decoder().getId());
		} catch (IOException e) {}
		
	}
}