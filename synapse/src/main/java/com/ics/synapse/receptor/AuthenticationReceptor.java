package com.ics.synapse.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.db.document.PoaState;
import com.ics.nceph.core.db.document.ProofOfAuthentication;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.AuthenticationMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.message.data.AuthenticationData;
import com.ics.nceph.core.message.data.CredentialsData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 03-Apr-2022
 */
public class AuthenticationReceptor extends Receptor 
{
	private AuthenticationData authenticationData;

	public AuthenticationReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			// 1. Get Authentication Data
			authenticationData = (AuthenticationData) message.decoder().getData(AuthenticationData.class);
		} catch (JsonProcessingException e) 
		{
			//LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.description("Class Name: " + this.getClass().getSimpleName())
					.action("Authentication data mapping failed")
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
			// 2.1 Set AUTHENTICATION read record
			poa.setAuthenticationReadRecord(getMessage().getReadRecord());
			// 2.2 Set STARTUP read record
			poa.setStartupReadRecord(authenticationData.getStartupReadRecord());
			// 2.3 Set STARTUP network record
			poa.setStartupNetworkRecord(authenticationData.getStartupNetworkRecord());
			// 2.4 Set AUTHENTICATION network record
			poa.setAuthenticationNetworkRecord(buildNetworkRecord());
			// 2.5 Set CREDENTIALS network record
			// TODO: (to be removed by Anshul after my checkin)
			poa.setCredentialsNetworkRecord(new NetworkRecord.Builder().start(new Date().getTime()).build());
			// 2.6 Set connection state
			poa.setPoaState(PoaState.AUTHENTICATE);
			// 2.7 Update the POA in the local DocumentStore
			DocumentStore.getInstance().update(poa, getMessage().decoder().getId());
			
			// 3. Create the CREDENTIALS message
			AuthenticationMessage credentialsMessage = new AuthenticationMessage.Builder()
					.messageId(getMessage().getMessageId()) // 3.1 Set incoming messageId
					.sourceId(getMessage().getSourceId()) // 3.2 Set incoming sourceId
					.type(SynapticOutgoingMessageType.CREDENTIALS.getMessageType()) // 3.3 Set message type
					.originatingPort(getMessage().getOriginatingPort())
					.data(new CredentialsData.Builder() // 3.3.1 Create the CREDENTIALS Data
							.startupWriteRecord(poa.getStartupWriteRecord()) // 3.3.2 Set STARTUP write record
							.authenticationReadRecord(getMessage().getReadRecord()) // 3.3.3 Set AUTHENTICATION read record
							.authenticationNetworkRecord(poa.getAuthenticationNetworkRecord()) // 3.3.4 Set AUTHENTICATION network record
							.credentialsNetworkRecord(poa.getCredentialsNetworkRecord().getStart()) // 3.3.5 Set CREDENTIALS network record  
							.credentials("NCEPH") // 3.3.6 Set credentials
							.build()) // 3.4 Set credentials data
					.build();
			
			// 4. Enqueue the message on the connection to be sent to the Cerebrum
			getIncomingConnection().enqueueMessage(credentialsMessage, QueuingContext.QUEUED_FROM_RECEPTOR);
			// 5. Change the interest of the connection to write
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		}catch(DocumentSaveFailedException | MessageBuildFailedException e) 
		{
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
				// Connection teardown
				getIncomingConnection().teardown();
			} catch (IOException e1) {
				// Log
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown failed")
						.logError(), e1);
			}			
			// Delete POA in local store
			poa.removeFromCache();
		}
	}
}
