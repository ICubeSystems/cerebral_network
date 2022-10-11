package com.ics.synapse.receptor;

import com.ics.env.Environment;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.receptor.PodReceptor;
import com.ics.synapse.message.type.SynapticIncomingMessageType;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * This {@link PodReceptor} is invoked when the synapse receives a {@link SynapticIncomingMessageType#DELETE_POD DELETE_POD} message. <br>
 * 
 * The incoming DELETE_POD messages is processed as follows:
 * <ol>
 * 	<li>Load {@link ProofOfPublish POD} from the local document store on the synapse</li>
 * 	<li>Update the POD with following information and save it to the local document store (update is only required in case the delete operation fails):
 * 		<ol>
 * 			<li>{@link NetworkRecord threeWayAckNetworkRecord} of the {@link SynapticOutgoingMessageType#ACK_RECEIVED ACK_RECEIVED} message. This was calculated on cerebrum & is sent back for logging to synapse</li>
 *			<li>Increment the delePod attempts</li>
 *			<li>Set the POD state to {@link MessageDeliveryState.FINISHED FINISHED} </li>
 *		</ol>
 * 	</li>
 * 	<li>Delete the POD from the local document store</li>
 * </ol>
 * <br>
 * 
 * @author Anshul
 * @version 1.0
 * @since 30-Mar-2022
 */
public class DeletePodReceptor extends PodReceptor 
{
	public DeletePodReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// 1. Load the POD to delete
		ProofOfPublish pod = ProofOfPublish.load(getMessage().decoder().getOriginatingPort(), getMessage().decoder().getId());
		if (pod == null)
		{
			// Log and return
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("404 - POD not found")
					.logInfo());
			return;
		}
		
		// 2. Set ThreeWayAckNetworkRecord and save to local storage. Save is only required in case the delete operation fails
		pod.setThreeWayAckMessageNetworkRecord(getPod().getThreeWayAckNetworkRecord());
		// 2.4 Set the delePod attempts
		pod.incrementFinalMessageAttempts();
		// 2.5 Set Pod State to FINISHED
		pod.setMessageDeliveryState(MessageDeliveryState.FINISHED.getState());
		try {
			DocumentStore.getInstance().update(pod, getMessage().decoder().getId());
			// MOCK CODE: to test the reliable delivery of the messages
			if(Environment.isDev() && pod.getMessageId().equals("1-15")) {
				System.out.println("forceStop"+getMessage().decoder().getId());
				return;
			}
			// END MOCK CODE
		} catch (DocumentSaveFailedException e) {}

		// 3. Delete the POD from local storage
		pod.removeFromCache();
	}
}
