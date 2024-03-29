package com.ics.cerebrum.receptor;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.PriorityBlockingQueue;

import com.ics.cerebrum.connector.CerebralConnector;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.ConnectionState;
import com.ics.nceph.core.db.document.PoaState;
import com.ics.nceph.core.db.document.ProofOfAuthentication;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ReadyConfirmedData;
import com.ics.nceph.core.receptor.Receptor;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 18-May-2022
 */
public class ReadyConfirmedReceptor extends Receptor
{
	private ReadyConfirmedData readyConfirmData;

	public ReadyConfirmedReceptor(Message message, Connection incomingConnection) {
		super(message, incomingConnection);
		try
		{
			// 1. Get Ready Data
			readyConfirmData = (ReadyConfirmedData) message.decoder().getData(ReadyConfirmedData.class);
		} catch (Exception e) 
		{
			// LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.description("Class Name: " + this.getClass().getSimpleName())
					.action("readyConfirm data mapping failed")
					.logError(),e);
		}
	}

	@Override
	public void process() 
	{
		CerebralConnector connector = (CerebralConnector)getIncomingConnection().getConnector();
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
		try 
		{
			// 2.1 Set CREDENTIALS write record
			poa.setCredentialsWriteRecord(readyConfirmData.getCredentialsWriteRecord());
			// 2.2 Set READY read record
			poa.setReadyReadRecord(readyConfirmData.getReadyReadRecord());
			// 2.3 Set READY network record
			poa.setReadyNetworkRecord(readyConfirmData.getReadyNetworkRecord());
			// 2.4 Set delete POA time
			poa.setDeletePoaTime(new Date().getTime());
			// 2.5 Set Connection State 
			poa.setPoaState(PoaState.READYCONFIRMED);
			// 2.6 Set READYCONFIRMED NetworkRecord
			poa.setReadyConfirmedNetworkRecord(buildNetworkRecord());
			// 2.7 Set READYCONFIRMED readRecord
			poa.setReadyConfirmedReadRecord(getMessage().getReadRecord());
			// 2.8 Update the POA in the local DocumentStore
			DocumentStore.getInstance().update(poa, getMessage().decoder().getId());

			// 3 Set incoming connection state READY
			getIncomingConnection().setState(ConnectionState.READY);
			// 3.1 Add the connection object to load balancer for read/ write allocations
			getIncomingConnection().addToLoadBalancer();
			// 3.2 Add the connection to the activeConnections list
			connector.getActiveConnections().put(getIncomingConnection().getId(), getIncomingConnection());
			
			// 4 Manage node wise connections mapping for this connector
			PriorityBlockingQueue<Connection> connections = connector.getNodeWiseConnectionsMap().get(getMessage().decoder().getSourceId());
			// 4.1 If first connection from this node then create a new PriorityBlockingQueue for connections from this node
			if(connections == null)
			{
				connections = new PriorityBlockingQueue<Connection>();
				connector.getNodeWiseConnectionsMap().put(getMessage().decoder().getSourceId(), connections);
			}
			//4.2 Add the connection to the PriorityBlockingQueue for this node
			connections.add(getIncomingConnection());
			
			NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
					.connectionId(String.valueOf(getIncomingConnection().getId()))
					.action("Ready connection")
					.data(new LogData()
							.entry("state", String.valueOf(getIncomingConnection().getState().getValue()))
							.entry("Port", String.valueOf(getIncomingConnection().getConnector().getPort()))
							.toString())
					.logInfo());
		} catch (DocumentSaveFailedException e) 
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
			} catch (IOException e1) 
			{
				// Log
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown failed")
						.logError(), e1);
			}

		}
		// Delete POA in local store
		poa.removeFromCache();
	}
}
