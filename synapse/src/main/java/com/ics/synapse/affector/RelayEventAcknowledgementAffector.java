package com.ics.synapse.affector;

import java.io.IOException;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfRelay;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 10-Apr-2022
 */
public class RelayEventAcknowledgementAffector extends Affector 
{

	public RelayEventAcknowledgementAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// Load the POR for this message
		ProofOfRelay por =  (ProofOfRelay) DocumentStore.load(ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
		if (por == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POR not found")
					.logInfo());
			return;
		}
		
		por.setAckWriteRecord(getMessage().getWriteRecord());
		// Save the POD
		try {
			DocumentStore.update(por, ProofOfRelay.DOC_PREFIX + getMessage().decoder().getId());
		} catch (IOException e) {}
		
	}
}
