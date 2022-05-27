package com.ics.cerebrum.affector;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PoaState;
import com.ics.nceph.core.document.ProofOfAuthentication;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.Message;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 08-Apr-2022
 */
public class ReadyAffector extends Affector 
{
	public ReadyAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load the POA for this authMessage
		ProofOfAuthentication poa = (ProofOfAuthentication) DocumentStore.load(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId());
		if (poa == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POA not found")
					.logInfo());
			return;
		}
		if(poa.getConnectionMessageState().getState() < PoaState.READY.getState()) 
		{
			// 1.1 Set READY write record
			poa.setReadyWriteRecord(getMessage().getWriteRecord());
			// 1.2 Set connection state
			poa.setConnectionMessageState(PoaState.READY);
			// 2. Update the POA in the local DocumentStore
			try {
				DocumentStore.update(poa, ProofOfAuthentication.DOC_PREFIX  + getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e) {}
		}
	}
}
