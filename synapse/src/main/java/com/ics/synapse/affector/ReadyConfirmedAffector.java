package com.ics.synapse.affector;

import java.util.Date;
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
 * @since 12-Apr-2022
 */
public class ReadyConfirmedAffector extends Affector
{
	public ReadyConfirmedAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		ProofOfAuthentication poa = (ProofOfAuthentication) DocumentStore.load(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId());
		if (poa == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POA not found")
					.logInfo());
			return;
		}
		if(poa.getConnectionMessageState().getState() < PoaState.READYCONFIRMED.getState())
		{
			// 1 Set connection state
			poa.setConnectionMessageState(PoaState.READYCONFIRMED);
			// 1.2 Set READYCONFIRMED WriteRecord
			poa.setReadyConfirmedWriteRecord(getMessage().getWriteRecord());
			// 1.3 Set delete POA time
			poa.setDeletePoaTime(new Date());
			try {
				// 2. Update the POA in the local storage
				DocumentStore.update(poa, ProofOfAuthentication.DOC_PREFIX  + getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e) {}
		}
		// 3. Delete POA
		DocumentStore.delete(ProofOfAuthentication.DOC_PREFIX  + getMessage().decoder().getId(),poa);
	}
}
