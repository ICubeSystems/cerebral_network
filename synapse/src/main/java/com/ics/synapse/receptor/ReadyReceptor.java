package com.ics.synapse.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;

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
import com.ics.nceph.core.message.data.ReadyConfirmedData;
import com.ics.nceph.core.message.data.ReadyData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 12-Apr-2022
 */
public class ReadyReceptor extends Receptor
{
	private ReadyData readyData;
	
	public ReadyReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			// 1. Get Ready Data
			readyData = (ReadyData) message.decoder().getData(ReadyData.class);
		} catch (Exception e) 
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
		try {
			// 2.1 Set CREDENTIALS network record
			poa.setCredentialsNetworkRecord(readyData.getCredentialsNetworkRecord());
			// 2.2 Set READY read record
			poa.setReadyReadRecord(getMessage().getReadRecord());
			// 2.3 Set CREDENTIALS read record
			poa.setCredentialsReadRecord(readyData.getCredentialsReadRecord());
			// 2.4 Set AUTHENTICATION write record
			poa.setAuthenticationWriteRecord(readyData.getAuthenticationWriteRecord());
			// 2.5 Set READY network record
			poa.setReadyNetworkRecord(buildNetworkRecord());
			// 2.6 Set connection state
			poa.setPoaState(PoaState.READY);
			// 2.7 Update the POA in the local DocumentStore
			DocumentStore.update(poa, ProofOfAuthentication.DOC_PREFIX  + getMessage().decoder().getId());
			
			// 3.  Set incoming connection state READY
			getIncomingConnection().setState(ConnectionState.PRE_READY);
			// 3.1 Add the connection object to load balancer for read/ write allocations
			getIncomingConnection().addToLoadBalancer();
			getIncomingConnection().getConnector().getActiveConnections().put(getIncomingConnection().getId(), getIncomingConnection());

			NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
					.connectionId(String.valueOf(getIncomingConnection().getId()))
					.action("ready connection")
					.data(new LogData()
							.entry("state", String.valueOf(getIncomingConnection().getState().getValue()))
							.entry("Port", String.valueOf(getIncomingConnection().getConnector().getPort()))
							.toString())
					.logInfo());

			// 4. Create the READY message
			AuthenticationMessage readyConfirmMessage = new AuthenticationMessage
					.Builder()
					.messageId(getMessage().getMessageId()) // 4.1 Set incoming messageId
					.sourceId(getMessage().getSourceId()) // 4.2 Set incoming sourceId
					.type(SynapticOutgoingMessageType.READY_CONFIRM.getMessageType()) // 4.3 Set message type
					.data(new ReadyConfirmedData // 5. Create READYCONFIRMED Data
							.Builder()
							.credentialsWriteRecord(poa.getCredentialsWriteRecord()) // 5.1 Set CREDENTIALS write record
							.readyReadRecord(getMessage().getReadRecord()) // 5.2 Set READY read record
							.readyNetworkRecord(poa.getReadyNetworkRecord()) // 5.3 Set READY network record
							.build()) // 4.4 Set READY event data
					.build();
			// 4.5 Enqueue the message on the connection to be sent to the cerebrum
			getIncomingConnection().enqueueMessage(readyConfirmMessage, QueuingContext.QUEUED_FROM_RECEPTOR);
			// 4.6 Change the interest of the connection to write
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		}catch(DocumentSaveFailedException | MessageBuildFailedException e) 
		{
			try {
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
			DocumentStore.delete(ProofOfAuthentication.DOC_PREFIX  + getMessage().decoder().getId(),poa);
		}
	}
}
