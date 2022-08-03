package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
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
import com.ics.nceph.core.message.data.AcknowledgementDoneData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.ThreeWayAcknowledgementReceptor;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 30-Mar-2022
 */
public class PublishedEventThreeWayAcknowledgementReceptor extends ThreeWayAcknowledgementReceptor 
{
	public PublishedEventThreeWayAcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Save the write record and three way acknowledgement record in the local datastore
		ProofOfDelivery pod = (ProofOfDelivery) DocumentStore.load(getMessage().decoder().getId());
		// Following are the cases when the POD will not be found for the message id:
		// a. POD is uploaded to the DB and 3-way ack is received after that [In this case we should check the DB and send Delete message if POD exists in DB]
		// b. POD is deleted by mistake on the synaptic node [Handling TBD - for now just logging such occurrence]
		if (pod == null)
		{
			// TODO: Handle case a.
			// Log the fatal error if the POD is not in the local storage and not in DB
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}

		try 
		{
			// 2. update the POD
			// 2.1 set ThreeWayAckReadRecord
			pod.setThreeWayAckReadRecord(getMessage().getReadRecord());
			// 2.2 set PUBLISH_EVENT WriteRecord which is sent in ACK_RECEIVED message body
			pod.setWriteRecord(getThreeWayAcknowledgement().getWriteRecord());
			// 2.3 set ACK_RECEIVED network record
			pod.setThreeWayAckNetworkRecord(buildNetworkRecord());
			// 2.4 set NCEPH_EVENT_ACK network record which is sent in ACK_RECEIVED message body
			pod.setAckNetworkRecord(getThreeWayAcknowledgement().getAckNetworkRecord());
			// 2.5 Set the threeWayAck attempts
			pod.incrementThreeWayAckAttempts();
			// 2.6 Set the delePod attempts
			pod.incrementDeletePodAttempts();
			// 2.7 Set Pod State to ACK_RECIEVED
			pod.setPodState(PodState.ACK_RECIEVED);
			// 2.8 Update the POD in the local storage
			DocumentStore.update(pod, getMessage().decoder().getId());


			// 3.1 Create the DELETE_POD message 			
			Message message = new AcknowledgeMessage.Builder()
					.data(new AcknowledgementDoneData.Builder()
							.threeWayAckNetworkRecord(buildNetworkRecord())
							.build())
					.messageId(getMessage().getMessageId())
					.type(CerebralOutgoingMessageType.DELETE_POD.getMessageType())
					.sourceId(getMessage().getSourceId())
					.build();
			// 3.2 Enqueue DELETE_POD on the incoming connection
			getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		} 
		catch (DocumentSaveFailedException e){} 
		catch (MessageBuildFailedException e1) 
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("DELETE_POD build failed")
					.logError(),e1);
			// decrement acknowledgement attempts in the pod		
			pod.decrementDeletePodAttempts();
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
						.description("DeletePod counter decrement failed after MessageBuildFailedException")
						.logError());
			}
			return;
		}
	}
}
