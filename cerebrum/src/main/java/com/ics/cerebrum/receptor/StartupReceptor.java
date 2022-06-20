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
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PoaState;
import com.ics.nceph.core.document.ProofOfAuthentication;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.AuthenticationMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.message.data.AuthenticationData;
import com.ics.nceph.core.message.data.StartupData;
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
	private StartupData startupData;

	public StartupReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try
		{
			// 1. Get Startup Data
			startupData = (StartupData) message.decoder().getData(StartupData.class);
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
		// 2.1 If the ProofOfAuthenticate for the received message is not in the local storage then create a new ProofOfAuthenticate object for this message 
		if (poa == null) 
		{
			try 
			{
				// 2.2 Build ProofOfAuthenticate object for this message
				poa = new ProofOfAuthentication.Builder()
						.messageId(getMessage().decoder().getId())
						.createdOn(startupData.getCreatedOn())
						.build();
				// 2.3 Set STARTUP read record
				poa.setStartupReadRecord(getMessage().getReadRecord());
				// 2.4 Set STARTUP network record
				poa.setStartupNetworkRecord(buildNetworkRecord());
				// 2.5 Set AUTHENTICATION network record
				// TODO: (to be removed by Anshul after my checkin)
				poa.setAuthenticationNetworkRecord(new NetworkRecord.Builder().start(new Date().getTime()).build());
				// 2.6 Set connection state
				poa.setPoaState(PoaState.STARTUP);
				// 2.7 Save the POA in the local DocumentStore
				DocumentStore.save(poa, ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId());
				
				// 3 Create the AUTHENTICATION Message
				AuthenticationMessage authenticationMessage = new AuthenticationMessage.Builder()
						.sourceId(getMessage().getSourceId()) // 3.1 Set incoming sourceId
						.messageId(getMessage().getMessageId()) // 3.2 Set incoming messageId
						.type(CerebralOutgoingMessageType.AUTHENTICATE.getMessageType()) // 3.3 Set message type
						.data(new AuthenticationData.Builder() // 3.3.1 Create the AUTHENTICATION Data
								.startupReadRecord(getMessage().getReadRecord()) // 3.3.2 Set STARTUP read record
								.startupNetworkRecord(poa.getStartupNetworkRecord()) // 3.3.3 Set STARTUP network record
								.authenticationNetworkRecord(poa.getAuthenticationNetworkRecord().getStart()) // 3.3.4 Set AUTHENTICATION network record
								.build()) // 3.4 Set authenticationMessage data
						.build();
				
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
				DocumentStore.delete(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId(), poa);
			}
		} else {
			//  If POA already created ?
		}
	}
}
