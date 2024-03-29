package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;

import com.ics.cerebrum.message.type.CerebralIncomingMessageType;
import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.message.data.AcknowledgementDoneData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.ThreeWayAcknowledgementReceptor;

/**
 * This {@link ThreeWayAcknowledgementReceptor} is invoked when the cerebrum receives a {@link CerebralIncomingMessageType#ACK_RECEIVED ACK_RECEIVED} message. <br>
 * 
 * The incoming ACK_RECEIVED messages is processed as follows:
 * <ol>
 * 	<li>Load {@link ProofOfPublish POD} from the local document store on the cerebrum</li>
 * 	<li>Update the POD with following information and save it to the local document store:
 * 		<ol>
 * 			<li>{@link IORecord threeWayAckReadRecord} of the {@link CerebralIncomingMessageType#ACK_RECEIVED ACK_RECEIVED} message on the cerebrum</li>
 * 			<li>{@link IORecord writeRecord} of the {@link CerebralIncomingMessageType#PUBLISH_EVENT PUBLISH_EVENT} message on the synapse</li>
 * 			<li>{@link NetworkRecord threeWayAckNetworkRecord} of the {@link CerebralIncomingMessageType#ACK_RECEIVED ACK_RECEIVED} message. Calculate, save and send it to synapse via {@link CerebralOutgoingMessageType#DELETE_POD DELETE_POD} message</li>
 * 			<li>{@link NetworkRecord ackNetworkRecord} of the {@link CerebralOutgoingMessageType#NCEPH_EVENT_ACK NCEPH_EVENT_ACK} message on the synapse. This was calculated on synapse & is sent back for logging to cerebrum</li>
 *			<li>Increment the threeWayAck & delePod attempts</li>
 *			<li>Set the POD state to {@link MessageDeliveryState.ACK_RECIEVED ACK_RECIEVED} </li>
 *		</ol>
 * 	</li>
 * 	<li>Enqueue the {@link CerebralOutgoingMessageType#DELETE_POD DELETE_POD} message to be sent back to the synapse, notifying that the acknowledgement has been received and instructing to delete the POD from their local document store</li>
 * </ol>
 * <br>

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
		ProofOfPublish pod = ProofOfPublish.load(getMessage().decoder().getOriginatingPort(), getMessage().decoder().getId());
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
			pod.setThreeWayAckMessageReadRecord(getMessage().getReadRecord());
			// 2.2 set PUBLISH_EVENT WriteRecord which is sent in ACK_RECEIVED message body
			pod.setEventMessageWriteRecord(getThreeWayAcknowledgement().getWriteRecord());
			// 2.3 set ACK_RECEIVED network record
			pod.setThreeWayAckMessageNetworkRecord(buildNetworkRecord());
			// 2.4 set NCEPH_EVENT_ACK network record which is sent in ACK_RECEIVED message body
			pod.setAckMessageNetworkRecord(getThreeWayAcknowledgement().getAckNetworkRecord());
			// 2.5 Set the threeWayAck attempts
			pod.incrementThreeWayAckMessageAttempts();
			// 2.6 Set the delePod attempts
			pod.incrementFinalMessageAttempts();
			// 2.7 Set Pod State to ACK_RECIEVED
			pod.setMessageDeliveryState(MessageDeliveryState.ACK_RECIEVED.getState());
			// 2.8 Update the POD in the local storage
			DocumentStore.getInstance().update(pod, getMessage().decoder().getId());


			// 3.1 Create the DELETE_POD message 			
			Message message = new AcknowledgeMessage.Builder()
					.data(new AcknowledgementDoneData.Builder()
							.threeWayAckNetworkRecord(buildNetworkRecord())
							.build())
					.messageId(getMessage().getMessageId())
					.type(CerebralOutgoingMessageType.DELETE_POD.getMessageType())
					.sourceId(getMessage().getSourceId())
					.originatingPort(getMessage().getOriginatingPort())
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
			pod.decrementFinalMessageAttempts();
			// Save the POD
			try 
			{
				DocumentStore.getInstance().update(pod, getMessage().decoder().getId());
			} catch (DocumentSaveFailedException e) 
			{
				//Log
				NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
						.messageId(String.valueOf(getMessage().decoder().getId()))
						.action("Pod updation failed")
						.description("DeletePod counter decrement failed after MessageBuildFailedException")
						.logError());
			}
		}
	}
}
