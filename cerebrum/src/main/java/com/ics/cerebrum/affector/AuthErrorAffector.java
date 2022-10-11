package com.ics.cerebrum.affector;

import java.io.IOException;
import java.util.Date;

import com.ics.logger.ConnectionLog;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.db.document.PoaState;
import com.ics.nceph.core.db.document.ProofOfAuthentication;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.Message;
/**
 * This class executes within a write worker thread after the channel write operation is done (after sending ERROR message).<br>
 * Updates following POA attributes:
 * <ol>
 * 	<li> <b>state:</b> set to AUTH_ERROR only if it is not yet AUTH_ERROR.</li>
 *  <li> <b>AuthenticationErrorWriteRecord:</b> Time taken (IORecord) to write the ERROR message on the channel </li>
 * </ol> 
 * @author Chandan Verma
 * @version 1.0
 * @since 09-Apr-2022
 */
public class AuthErrorAffector extends Affector
{	
	public AuthErrorAffector(Message message, Connection incomingConnection) 
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
		if(poa.getPoaState().getState() < PoaState.AUTH_ERROR.getState()) 
		{
			// 1.1 Set delete POA time
			poa.setDeletePoaTime(new Date().getTime());
			// 1.2 Set AUTH_ERROR write record
			poa.setAuthenticationErrorWriteRecord(getMessage().getWriteRecord());
			// 1.3 Set connection state
			poa.setPoaState(PoaState.AUTH_ERROR);
			// 1.4 Update the POA in the local DocumentStore
			try 
			{
				DocumentStore.getInstance().update(poa, getMessage().decoder().getId());
				// Log
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown")
						.logInfo());
				// Connection teardown
				getIncomingConnection().teardown();
			} catch (IOException e) {
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown failed")
						.data("Connection state AUTH_FAILED, connection teardown")
						.logError(), e);
			}
		}
	}
}
