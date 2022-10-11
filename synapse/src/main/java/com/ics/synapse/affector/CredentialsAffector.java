package com.ics.synapse.affector;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.db.document.PoaState;
import com.ics.nceph.core.db.document.ProofOfAuthentication;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.Message;
/**
 * This class executes within a write worker thread after the channel write operation is done (after sending CREDENTIALS message).<br>
 * Updates following POA attributes:
 * <ol>
 * 	<li> <b>state:</b> set to CREDENTIALS only if it is not yet CREDENTIALS.</li>
 *  <li> <b>CredentialsWriteRecord:</b> Time taken (IORecord) to write the CREDENTIALS message on the channel </li>
 * </ol>
 * @author Chandan Verma
 * @version 1.0
 * @since 04-Apr-2022
 */
public class CredentialsAffector extends Affector
{
	public CredentialsAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load the POA for this authMessage
		ProofOfAuthentication poa = ProofOfAuthentication.load(getMessage().decoder().getOriginatingPort(), getMessage().decoder().getId());
		if (poa == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POA not found")
					.logInfo());
			return;
		}
		if(poa.getPoaState().getState() < PoaState.CREDENTIALS.getState()) 
		{
			// 1.1 Set CREDENTIALS read record
			poa.setCredentialsWriteRecord(getMessage().getWriteRecord());
			// 1.2 Set connection state
			poa.setPoaState(PoaState.CREDENTIALS);
			try {
				// 3. Update the POA in the local storage
				DocumentStore.getInstance().update(poa, getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e) {}
		}
	}
}
