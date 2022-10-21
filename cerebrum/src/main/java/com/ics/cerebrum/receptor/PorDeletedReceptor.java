package com.ics.cerebrum.receptor;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.receptor.PodReceptor;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 10-Apr-2022
 */
public class PorDeletedReceptor extends PodReceptor 
{
	public PorDeletedReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load the POD to Update RELAY_ACK_RECEIVED network record.
		ProofOfPublish pod = ProofOfPublish.load(getMessage().decoder().getOriginatingPort(), getMessage().decoder().getId());
		ProofOfRelay por = ProofOfRelay.load(getMessage().decoder().getOriginatingPort(), getIncomingConnection().getConnector().getPort(), getMessage().decoder().getId());
		if (por == null)
		{
			// Log and return
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}

		// 2. Increment relay count only if porstate is not finished already.
		if(por.getMessageDeliveryState() != MessageDeliveryState.FINISHED.getState())
			pod.incrementRelayCount();
		por.setMessageDeliveryState(MessageDeliveryState.FINISHED.getState());
		// 3. Set ThreeWayRelayAckNetworkRecord and save to local storage.
		por.setThreeWayAckMessageNetworkRecord(getPod().getThreeWayAckNetworkRecord());
		por.incrementFinalMessageAttempts();
		por.setAppReceptorFailed(false);
		if(pod.getRelayCount()==pod.getSubscriberCount())
			pod.setMessageDeliveryState( MessageDeliveryState.FULLY_RELAYED.getState() );
		try {
			DocumentStore.getInstance().update(pod, getMessage().decoder().getId());
			DocumentStore.getInstance().update(por, getMessage().decoder().getId());
		} catch (DocumentSaveFailedException e) {}
		por.removeFromCache();
		pod.finished();
	}
}
