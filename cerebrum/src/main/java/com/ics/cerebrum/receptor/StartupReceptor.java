package com.ics.cerebrum.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Date;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.db.document.PoaState;
import com.ics.nceph.core.db.document.ProofOfAuthentication;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.AuthenticationMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AuthenticationData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.Receptor;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 29-Mar-2022
 */
public class StartupReceptor extends Receptor 
{
	public StartupReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		ProofOfAuthentication poa = null;
		try 
		{
			// 1 Build ProofOfAuthenticate object for this message
			poa = new ProofOfAuthentication.Builder()
					.messageId(getMessage().decoder().getId())
					.createdOn(new Date().getTime())
					.originatingPort(getMessage().decoder().getOriginatingPort())
					.build();
			// 1.2 Set STARTUP read record
			poa.setStartupReadRecord(getMessage().getReadRecord());
			// 1.3 Set STARTUP network record
			poa.setStartupNetworkRecord(buildNetworkRecord());
			// 1.4 Set connection state
			poa.setPoaState(PoaState.STARTUP);
			// 1.5 Save the POA in the local DocumentStore
			DocumentStore.getInstance().save(poa, getMessage().decoder().getId());

			// 2 Create the AUTHENTICATION Message
			AuthenticationMessage authenticationMessage = new AuthenticationMessage.Builder()
					.sourceId(getMessage().getSourceId()) // 3.1 Set incoming sourceId
					.messageId(getMessage().getMessageId()) // 3.2 Set incoming messageId
					.originatingPort(getMessage().getOriginatingPort())// set originating port
					.type(CerebralOutgoingMessageType.AUTHENTICATE.getMessageType()) // 3.3 Set message type
					.data(new AuthenticationData.Builder() // 3.3.1 Create the AUTHENTICATION Data
							.startupReadRecord(getMessage().getReadRecord()) // 3.3.2 Set STARTUP read record
							.startupNetworkRecord(poa.getStartupNetworkRecord()) // 3.3.3 Set STARTUP network record
							.build()) // 3.4 Set authenticationMessage data
					.build();
			// 3 set application node id to connection
			getIncomingConnection().setNodeId(getMessage().decoder().getSourceId());
			// 4 Enqueue AUTHENTICATION for sending
			getIncomingConnection().enqueueMessage(authenticationMessage, QueuingContext.QUEUED_FROM_RECEPTOR);
			// 5 Set the interest of the connection to write
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
				// Request teardown
				getIncomingConnection().teardown();
			} catch (IOException e1) 
			{
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