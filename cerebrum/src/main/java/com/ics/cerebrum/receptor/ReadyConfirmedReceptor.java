package com.ics.cerebrum.receptor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.Date;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PoaState;
import com.ics.nceph.core.document.ProofOfAuthentication;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
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
			e.printStackTrace();
		}
	}

	@Override
	public void process() 
	{
		// 2. Load POA in the local DocumentStore
		ProofOfAuthentication	poa = (ProofOfAuthentication) DocumentStore.load(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId());
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
			// TODO: (to be removed by Anshul after my checkin)
			// 2.3 Set READY network record
			poa.setReadyNetworkRecord(readyConfirmData.getReadyNetworkRecord());
			// TODO 2.4 Set delete POA time
			poa.setDeletePoaTime(new Date());
			// 2.5 Set Connection State 
			poa.setConnectionMessageState(PoaState.READYCONFIRMED);
			// TODO: (to be removed by Anshul after my checkin)
			// 2.6 Set READYCONFIRMED NetworkRecord
			poa.setReadyConfirmedNetworkRecord(new NetworkRecord.Builder().start(readyConfirmData.getReadyConfirmedNetwork()).end(new Date()).build());
			// 2.7 Set READYCONFIRMED readRecord
			poa.setReadyConfirmedReadRecord(getMessage().getReadRecord());
			// 2.8 Update the POA in the local DocumentStore
			DocumentStore.update(poa, ProofOfAuthentication.DOC_PREFIX  + getMessage().decoder().getId());
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
			// Delete POA in local store
			DocumentStore.delete(ProofOfAuthentication.DOC_PREFIX + getMessage().decoder().getId(), poa);
			return ;
		}

		// 3. If there are outgoing messages in the buffer then transfer them to the connections relayQueue
		if (getIncomingConnection().getConnector().getRelayQueue().size() > 0)
		{
			// Log
			NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
					.connectionId(String.valueOf(getIncomingConnection().getId()))
					.action("Enqueueing")
					.data(new LogData()
							.entry("Connection ID", String.valueOf(getIncomingConnection().getId()))
							.entry("Port", String.valueOf(getIncomingConnection().getConnector().getPort()))
							.entry("Relay size", String.valueOf(getIncomingConnection().getConnector().getRelayQueue().size()))
							.toString())
					.description("messages from the outgoing buffer (relayQueue) to connection's relayQueue")
					.logInfo());


			while(!getIncomingConnection().getConnector().getRelayQueue().isEmpty()) {
				NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("Enqueueing")
						.data(new LogData()
								.entry("Relay size", String.valueOf(getIncomingConnection().getConnector().getRelayQueue().size()))
								.toString())
						.description("messages from the outgoing buffer (relayQueue) to connection's relayQueue")
						.logInfo());

				for (int i = 0; i < 10 && !getIncomingConnection().getConnector().getRelayQueue().isEmpty(); i++)
					getIncomingConnection().enqueueMessage(getIncomingConnection().getConnector().getRelayQueue().poll());
				getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
			}
		}
	}
}
