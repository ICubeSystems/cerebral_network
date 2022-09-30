package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.env.Environment;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ThreeWayAcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.AcknowledgementReceptor;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 10-Apr-2022
 */
public class RelayedEventAcknowledgeReceptor extends AcknowledgementReceptor 
{
	public RelayedEventAcknowledgeReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{

		// 1. Load POD: Ideally POD will always be loaded for the ack message
		ProofOfRelay por = ProofOfRelay.load(getMessage().decoder().getOriginatingPort(), getIncomingConnection().getConnector().getPort(), getMessage().decoder().getId());
		// Following are the cases when the POD will not be found for the message id:
		// a. POD is deleted by mistake on the synaptic node
		// b. POD was deleted properly following the DELETE message from cerebrum, but cerebrum has repeated the ACK for this message
		// TODO: Handle here - For now just log such occurrence. Real handling - TBD after we gather some more data for the same
		if (por == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POR not found")
					.logInfo());
			return;
		}
		

		// 1.1 Make sure that WriteRecord is written in the POR. If not then try to load again after 1000 ms. 
		// This happens when the RelayedEventAffector executes after RelayedEventAcknowledgeReceptor	
		while(por.getEventMessageWriteRecord()==null) 
		{
			try 
			{
				//Sleep
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
			// 1.2 Load the POD again
			por = ProofOfRelay.load(getMessage().decoder().getOriginatingPort(), getIncomingConnection().getConnector().getPort(), getMessage().decoder().getId());
		}

		try
		{
			// 2. Update POR - create a NetworkRecord for RELAYED_EVENT_ACK
			// 2.1 Set RELAY_EVENT read record on the Synapse
			por.setEventMessageReadRecord(getAcknowledgement().getReadRecord());
			// 2.2 Set RELAYED_EVENT_ACK network record
			por.setAckMessageNetworkRecord(buildNetworkRecord());
			// 2.3 Set the acknowledgement attempts
			por.incrementAcknowledgementMessageAttempts();
			// 2.4 Set the threeWayAck attempts
			por.incrementThreeWayAckMessageAttempts();
			// 2.5 Set RELAY_EVENT network record on the synapse
			por.setEventMessageNetworkRecord(getAcknowledgement().getEventNetworkRecord());
			// 2.6 Set RELAYED_EVENT_ACK read record 
			por.setAckMessageReadRecord(getMessage().getReadRecord());
			// 2.7 Set Por State to ACKNOWLEDGED
			por.setMessageDeliveryState(MessageDeliveryState.ACKNOWLEDGED.getState());
			// 2.8 Set event application receptor name
			por.setAppReceptorName(getAcknowledgement().getAppReceptorName());
			// 2.9 Set application receptor execution time
			por.setAppReceptorExecutionTime(getAcknowledgement().getAppReceptorExecutionTime());
			// 2.10 Set application receptor error message
			por.setAppReceptorExecutionErrorMsg(getAcknowledgement().getAppReceptorExecutionErrorMsg());
			// 2.11 Set application receptor error message
			por.setAppReceptorFailed(getAcknowledgement().isAppReceptorFailed());
			// 2.12 increment AppReceptorExecutionAttempts
			por.incrementAppReceptorExecutionAttempts();
			// 2.13 Update the POD in the local storage
			// 2.7 Set NodeId
			por.setConsumerNodeId(getAcknowledgement().getNodeId());
			// 2.8 Update the POD in the local storage
			DocumentStore.getInstance().update(por, getMessage().decoder().getId());

			// MOCK CODE: to test the reliable delivery of the messages
			if(Environment.isDev() && por.getMessageId().equals("1-30")) 
			{
				System.out.println("forceStop"+getMessage().decoder().getId());
				return;
			}
			// END MOCK CODE
			// 3.0 Create the message data for RELAY_ACK_RECEIVED message to be sent to synapse

			ThreeWayAcknowledgementData threeWayAck = new ThreeWayAcknowledgementData.Builder()
					.writeRecord(por.getEventMessageWriteRecord())
					.ackNetworkRecord(buildNetworkRecord()).build();
			// 3.1 Create the RELAY_ACK_RECEIVED message 
			Message message = new AcknowledgeMessage.Builder()
					.data(threeWayAck)
					.messageId(getMessage().getMessageId())
					.type(CerebralOutgoingMessageType.RELAY_ACK_RECEIVED.getMessageType())
					.sourceId(getMessage().getSourceId())
					.originatingPort(getMessage().getOriginatingPort())
					.build();
			// 3.2 Enqueue RELAY_ACK_RECEIVED on the incoming connection
			getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);

		} 
		catch (DocumentSaveFailedException e){} 
		catch (MessageBuildFailedException e1) 
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("RELAY_ACK_RECEIVED build failed")
					.logError(),e1);

			// decrement acknowledgement attempts in the pod		
			por.decrementThreeWayAckMessageAttempts();
			// Save the POD
			try 
			{
				DocumentStore.getInstance().update(por, getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e){}
			return;
		}

	}
}
