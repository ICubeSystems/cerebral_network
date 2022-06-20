package com.ics.synapse.receptor;

import java.nio.channels.SelectionKey;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PodState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ThreeWayAcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.AcknowledgementReceptor;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 29-Mar-2022
 */
public class EventAcknowledgementReceptor extends AcknowledgementReceptor 
{
	public EventAcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load POD: Ideally POD will always be loaded for the ack message
		ProofOfDelivery pod = (ProofOfDelivery) DocumentStore.load(getMessage().decoder().getId());

		// Following are the cases when the POD will not be found for the message id:
		// a. POD is deleted by mistake on the synaptic node
		// b. POD was deleted properly following the DELETE message from cerebrum, but cerebrum has repeated the ACK for this message
		// TODO: Handle here - For now just log such occurrence. Real handling - TBD after we gather some more data for the same
		if (pod == null)
		{
			NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}
		// 1.1 Make sure that WriteRecord is written in the POD. If not then try to load again after 1000 ms. 
		// This happens when the PublishEventAffector executes after EventAcknowledgementReceptor
		while(pod.getWriteRecord()==null) 
		{
			try 
			{
				//Sleep
				Thread.sleep(1000);
			} catch (InterruptedException e) {e.printStackTrace();}
			// 1.2 Load the POD again
			pod = (ProofOfDelivery) DocumentStore.load(getMessage().decoder().getId());
		}
		try 
		{

			// 2. Update POD
			// 2.1 Set PUBLISH_EVENT read record on the cerebrum
			pod.setReadRecord(getAcknowledgement().getReadRecord());
			// 2.2 Set NCEPH_EVENT_ACK read record on the synapse
			pod.setAckReadRecord(getMessage().getReadRecord());
			// 2.3 Set PUBLISH_EVENT network record on the synapse
			pod.setEventNetworkRecord(getAcknowledgement().getEventNetworkRecord());
			// 2.4 Set NCEPH_EVENT_ACK network record
			pod.setAckNetworkRecord(buildNetworkRecord());
			// 2.5 Set the acknowledgement attempts
			pod.incrementAcknowledgementAttempts();
			// 2.6 Set the threeWayAck attempts
			pod.incrementThreeWayAckAttempts();
			// 2.7 Set Pod State to ACKNOWLEDGED
			pod.setPodState(PodState.ACKNOWLEDGED);
			// 2.8 Update the POD in the local storage
			DocumentStore.update(pod, getMessage().decoder().getId());

			// 3.0 Create the message data for ACK_RECEIVED message to be sent to cerebrum
			// 3.1 Create the ACK_RECEIVED message 			
			Message message = new AcknowledgeMessage.Builder()
					.data(new ThreeWayAcknowledgementData.Builder()
							.writeRecord(pod.getWriteRecord()) // WriteRecord of PUBLISH_EVENT
							.ackNetworkRecord(buildNetworkRecord()) // NCEPH_EVENT_ACK network record
							.build())
					.messageId(getMessage().getMessageId())
					.type(SynapticOutgoingMessageType.ACK_RECEIVED.getMessageType())
					.sourceId(getMessage().getSourceId())
					.build();
			// 3.2 Enqueue ACK_RECEIVED on the incoming connection

			getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		} 
		catch (DocumentSaveFailedException e){} 
		catch (MessageBuildFailedException e1) 
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("ACK_RECEIVED build failed")
					.logError(),e1);
			// decrement acknowledgement attempts in the pod		
			pod.decrementThreeWayAckAttempts();
			// Save the POD
			try 
			{
				DocumentStore.update(pod, getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e) 
			{
				//Log
				NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
						.messageId(String.valueOf(getMessage().decoder().getId()))
						.action("Pod updation failed")
						.description("threeWayAck counter decrement failed after MessageBuildFailedException")
						.logError());
			}
			return;
		}
	}
}
