package com.ics.cerebrum.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.ConnectionState;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PoaState;
import com.ics.nceph.core.document.ProofOfAuthentication;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.AuthenticationMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AuthErrorData;
import com.ics.nceph.core.message.data.CredentialsData;
import com.ics.nceph.core.message.data.ReadyData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.Receptor;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 05-Apr-2022
 */
public class CredentialsReceptor extends Receptor
{
	private CredentialsData credentialsData;

	public CredentialsReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			// 1. Get Credentials Data
			credentialsData = (CredentialsData) message.decoder().getData(CredentialsData.class);
		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public void process() 
	{
		// 2. Load POA in the local DocumentStore
		ProofOfAuthentication poa = (ProofOfAuthentication) DocumentStore.load(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId());
		if (poa == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POA not found")
					.logInfo());
			return;
		}
		try 
		{
			// 2.1 set STARTUP write record 
			poa.setStartupWriteRecord(credentialsData.getStartupWriteRecord());
			// 2.2 Set AUTHENTICATION read record
			poa.setAuthenticationReadRecord(credentialsData.getAuthenticationReadRecord());
			// 2.3 Set CREDENTIALS read record
			poa.setCredentialsReadRecord(getMessage().getReadRecord());
			// 2.4 Set AUTHENTICATION network record
			poa.setAuthenticationNetworkRecord(credentialsData.getAuthenticationNetworkRecord());
			// 2.5 Set CREDENTIALS network record
			poa.setCredentialsNetworkRecord(buildNetworkRecord());
			// 2.6 Set connection state
			poa.setPoaState(PoaState.CREDENTIALS);
			// 2.7 Update the POA in the local DocumentStore
			DocumentStore.update(poa, ProofOfAuthentication.DOC_PREFIX  + getMessage().decoder().getId());
			
			// Check Credentials
			if(credentialsData.getCredentials().equals("NCEPH"))
			{
				// 3 Set incoming connection state READY
				getIncomingConnection().setState(ConnectionState.READY);
				// 3.1 Add the connection object to load balancer for read/ write allocations
				getIncomingConnection().getConnector().getConnectionLoadBalancer().add(getIncomingConnection());
				getIncomingConnection().getConnector().getActiveConnections().put(getIncomingConnection().getId(), getIncomingConnection());

				NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("Ready connection")
						.data(new LogData()
								.entry("state", String.valueOf(getIncomingConnection().getState().getValue()))
								.entry("Port", String.valueOf(getIncomingConnection().getConnector().getPort()))
								.toString())
						.logInfo());

				// 4. Ready Message
				// 4.1 Create the READY message
				AuthenticationMessage readyMessage = new AuthenticationMessage
						.Builder()
						.messageId(getMessage().getMessageId()) // 4.2 Set incoming messageId
						.sourceId(getMessage().getSourceId()) // 4.3 Set incoming sourceId
						.type(CerebralOutgoingMessageType.READY.getMessageType())
						.data(new ReadyData // 4.4.1 Create the READY Data
								.Builder()
								.credentialsNetworkRecord(poa.getCredentialsNetworkRecord()) // 4.4.2 Set CREDENTIALS network record
								.credentialsReadRecord(getMessage().getReadRecord()) // 4.4.3 Set CREDENTIALS read record
								.authenticationWriteRecord(poa.getAuthenticationWriteRecord())  // 4.4.4 Set AUTHENTICATION write record
								.build()) // 4.5 Set READY message data
						.build();
				// 4.6 Enqueue the message on the connection to be sent to the synapse
				getIncomingConnection().enqueueMessage(readyMessage, QueuingContext.QUEUED_FROM_RECEPTOR);
				// 4.7 Change the interest of the connection to write
				getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
			} else {
				// 5. Error Message
				
				// 5.1 Set incoming connection state AUTH_FAILED
				getIncomingConnection().setState(ConnectionState.AUTH_FAILED);

				NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("Auth_failed connection")
						.data(new LogData()
								.entry("state", String.valueOf(getIncomingConnection().getState().getValue()))
								.entry("Port", String.valueOf(getIncomingConnection().getConnector().getPort()))
								.toString())
						.logInfo());

				// 6. Create the AUTH_ERROR message
				AuthenticationMessage ErrorMessage = new AuthenticationMessage
						.Builder()
						.messageId(getMessage().getMessageId()) // 6.1 Set incoming messageId
						.sourceId(getMessage().getSourceId()) // 6.2 Set incoming sourceId
						.type(CerebralOutgoingMessageType.ERROR.getMessageType())
						.data(new AuthErrorData // 6.3.1 Create the AUTH_ERROR Data
								.Builder()
								.credentialsNetworkRecord(poa.getCredentialsNetworkRecord()) // 6.3.2 Set CREDENTIALS network record
								.credentialsReadRecord(getMessage().getReadRecord()) // 6.3.3 Set CREDENTIALS read record
								.authenticationWriteRecord(poa.getAuthenticationWriteRecord()) // 6.3.4 Set AUTHENTICATION write record
								.build()) // 6.4 Set ERROR message data
						.build();
				// 6.5 Enqueue the message on the connection to be sent to the synapse
				getIncomingConnection().enqueueMessage(ErrorMessage, QueuingContext.QUEUED_FROM_RECEPTOR);
				// 6.6 Change the interest of the connection to write
				getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
			}
		} catch(DocumentSaveFailedException | MessageBuildFailedException e) 
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
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown failed")
						.logError(), e1);
			}
			// Delete POA in local store
			DocumentStore.delete(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId(),poa);
		}
	}
}
