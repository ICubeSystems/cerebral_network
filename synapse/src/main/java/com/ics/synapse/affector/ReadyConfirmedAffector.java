package com.ics.synapse.affector;

import java.util.Date;
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
 * This class executes within a write worker thread after the channel write operation is done (after sending READY_CONFIRM message).<br>
 * Updates following POA attributes:
 * <ol>
 * 	<li> <b>state:</b> set to READYCONFIRMED only if it is not yet READYCONFIRMED.</li>
 *  <li> <b>ReadyConfirmedWriteRecord:</b> Time taken (IORecord) to write the READY_CONFIRM message on the channel </li>
 * </ol>
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
		ProofOfAuthentication poa = ProofOfAuthentication.load(getMessage().decoder().getOriginatingPort(), getMessage().decoder().getId());
		if (poa == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POA not found")
					.logInfo());
			return;
		}
		if(poa.getPoaState().getState() < PoaState.READYCONFIRMED.getState())
		{
			// 1 Set connection state
			poa.setPoaState(PoaState.READYCONFIRMED);
			// 1.2 Set READYCONFIRMED WriteRecord
			poa.setReadyConfirmedWriteRecord(getMessage().getWriteRecord());
			// 1.3 Set delete POA time
			poa.setDeletePoaTime(new Date().getTime());
			try {
				// 2. Update the POA in the local storage
				DocumentStore.getInstance().update(poa, getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e) {}
		}
		// 3. Delete POA
		poa.removeFromCache();
	}
}
