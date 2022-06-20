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
 * This class executes within a write worker thread after the channel write operation is done (after sending AUTHENTICATE message).<br>
 * Updates following POA attributes:
 * <ol>
 * 	<li> <b>state:</b> set to AUTHENTICATE only if it is not yet AUTHENTICATE.</li>
 *  <li> <b>AuthenticationWriteRecord:</b> Time taken (IORecord) to write the AUTHENTICATE message on the channel </li>
 * </ol>
 * @author Chandan Verma
 * @version 1.0
 * @since 03-Apr-2022
 */
public class AuthenticationAffector extends Affector 
{
	public AuthenticationAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load the POA for this authMessage
		ProofOfAuthentication poa = (ProofOfAuthentication) DocumentStore.load(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId());
		if(poa == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POA not found")
					.logInfo());
			return;
		}
		if(poa.getPoaState().getState() < PoaState.AUTHENTICATE.getState()) 
		{
			// 1.2 Set AUTHENTICATION write record
			poa.setAuthenticationWriteRecord(getMessage().getWriteRecord());
			// 1.3 Set connection state
			poa.setPoaState(PoaState.AUTHENTICATE);
			// 3. Update the POA in the local DocumentStore
			try {
				DocumentStore.update(poa, ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e) {}	
		}
	}
}
