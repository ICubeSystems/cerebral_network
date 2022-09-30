package com.ics.synapse.receptor;

import java.io.IOException;
import java.util.Date;

import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.ConnectionState;
import com.ics.nceph.core.db.document.PoaState;
import com.ics.nceph.core.db.document.ProofOfAuthentication;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AuthErrorData;
import com.ics.nceph.core.receptor.Receptor;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 15-Apr-2022
 */
public class AuthErrorReceptor extends Receptor
{
	private AuthErrorData authErrorData;

	public AuthErrorReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			// 1. Get Error Data
			authErrorData =	(AuthErrorData) message.decoder().getData(AuthErrorData.class);
		} catch (Exception e) 
		{
			//LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.description("Class Name: " + this.getClass().getSimpleName())
					.action("AuthErrorData mapping failed")
					.logError(),e);
		}
	}

	@Override
	public void process() 
	{
		// 2. Load POA in the local DocumentStore
		ProofOfAuthentication poa = ProofOfAuthentication.load(getMessage().decoder().getOriginatingPort(), getMessage().decoder().getId());
		if (poa == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POA not found")
					.logInfo());
			return;
		}
		try {
			// 2.1 Set CREDENTIALS network record
			poa.setCredentialsNetworkRecord(authErrorData.getCredentialsNetworkRecord());
			// 2.2 Set ERROR read record
			poa.setAuthenticationErrorReadRecord(getMessage().getReadRecord());
			// 2.3 Set CREDENTIALS read record
			poa.setCredentialsReadRecord(authErrorData.getCredentialsReadRecord());
			// 2.4 Set AUTHENTICATION write record
			poa.setAuthenticationWriteRecord(authErrorData.getAuthenticationWriteRecord());
			// 2.5 Set ERROR network record
			poa.setAuthenticationErrorNetworkRecord(buildNetworkRecord());
			// 2.6 Set delete POA time
			poa.setDeletePoaTime(new Date().getTime());
			// 2.7 Set connection state
			poa.setPoaState(PoaState.AUTH_ERROR);
			// 2.7 Update the POA in the local DocumentStore
			DocumentStore.getInstance().update(poa, getMessage().decoder().getId());
			// 2.8 Set incoming connection state AUTH_FAILED
			getIncomingConnection().setState(ConnectionState.AUTH_FAILED);

			NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
					.connectionId(String.valueOf(getIncomingConnection().getId()))
					.action("connection authentication failed ")
					.data(new LogData()
							.entry("state", String.valueOf(getIncomingConnection().getState().getValue()))
							.entry("Port", String.valueOf(getIncomingConnection().getConnector().getPort()))
							.toString())
					.logInfo());

			// Log
			NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
					.connectionId(String.valueOf(getIncomingConnection().getId()))
					.action("requesting teardown")
					.data(new LogData()
							.entry("Reason ", "Connection authentication failed")
							.toString())
					.logInfo());

			// 3. Close incomingConnection
			getIncomingConnection().teardown();
			// 4. Delete POA in local store 
			poa.removeFromCache();
		} catch (IOException e) {
			try 
			{	
				// Log
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown")
						.data(new LogData()
								.entry("Reason ", e.getMessage())
								.toString())
						.logInfo());
				// connection teardown
				getIncomingConnection().teardown();	
			} catch (IOException e1) {
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown failed")
						.logError(), e1);
			}
			poa.removeFromCache();
		}
	}
}
