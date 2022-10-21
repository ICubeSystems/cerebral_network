package com.ics.synapse.connector;

import java.util.Map;
import java.util.Map.Entry;

import com.ics.logger.MessageLog;
import com.ics.logger.MonitorLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.MonitorTask;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;
import com.ics.nceph.core.db.document.MessageDeliveryState;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ThreeWayAcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;
import com.ics.util.ByteUtil;

import lombok.Builder;

/**
 * This consumer/ function class to process {@link ProofOfPublish} documents to manage the publish of the message
 * 
 * @author Anshul
 * @version 1.0
 * @since Oct 17, 2022
 */
@Builder
public class MonitorPublish extends MonitorTask
{
	private SynapticConnector connector;

	@Override
	public void accept(Entry<String, ProofOfPublish> t)
	{
		processPods(t);
	}

	/**
	 * Check for uncompleted relay messages and process them by re-sending them according to their state 
	 * @throws ImproperMonitorInstantiationException
	 * @throws ImproperConnectorInstantiationException
	 * @since Oct 17, 2022
	 */
	private void processPods(Map.Entry<String, ProofOfPublish> entry) 
	{
		// get connection from connector active connections
		Connection connection;
		try
		{
			connection = connector.getConnection();
		} catch (ImproperConnectorInstantiationException e){return;}	// If there are no active connections in the connector then break.
		String messageId = entry.getKey();
		ProofOfPublish pod = (ProofOfPublish)entry.getValue();
		try 
		{
			// check pod file is older than x minutes. 
			if (transmissionWindowElapsed(pod)) 
			{
				// Handle case according to pod state of current pod
				switch (pod.getMessageDeliveryState()) 
				{
				case 100:// INITIAL state of POD
				case 200:// DELIVERED state of POD
					// Create PUBLISH_EVENT message 
					Message message1 = new EventMessage.Builder()
					.event(pod.getEvent())
					.mid(pod.getMessageId())
					.originatingPort(connector.getPort())
					.buildAgain();

					enqueueMessage(connection, message1);
					// Set the publish attempts
					pod.incrementEventMessageAttempts();
					// Set POD State to DELIVERED
					pod.setMessageDeliveryState(MessageDeliveryState.DELIVERED.getState());
					// Set POD State to PUBLISHED
					DocumentStore.getInstance().update(pod, messageId);
					break;
				case 300:// ACKNOWLEDGED state of POD
				case 400:// ACK_RECIEVED state of POD
					// Create the ACK_RECEIVED message  
					Message message = new AcknowledgeMessage.Builder()
					.data(new ThreeWayAcknowledgementData.Builder()
							.writeRecord(pod.getEventMessageWriteRecord()) // WriteRecord of PUBLISH_EVENT
							.ackNetworkRecord(pod.getAckMessageNetworkRecord()) // NCEPH_EVENT_ACK network record
							.build())
					.mid(pod.getMessageId())
					.originatingPort(ByteUtil.convertToByteArray(connector.getPort(), 2))
					.type(SynapticOutgoingMessageType.ACK_RECEIVED.getMessageType())
					.build();
					enqueueMessage(connection, message);
					// Set the threeWayAck attempts
					pod.incrementThreeWayAckMessageAttempts();
					// Set POD State to ACK_RECIEVED
					pod.setMessageDeliveryState(MessageDeliveryState.ACK_RECIEVED.getState());
					DocumentStore.getInstance().update(pod, messageId);
					break;
				case 500:// FINISHED state of POD
					// Delete the POD from local storage
					pod.removeFromCache();
					break;
				default:
					break;
				}

			}
		} 
		catch (MessageBuildFailedException e) 
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(messageId)
					.action(pod.getMessageDeliveryState() == 100 || pod.getMessageDeliveryState() == 200?"NCEPH_EVENT build failed":"ACK_RECEIVED build failed")
					.description("message build failed in monitor")
					.logError(),e);
			pod.decrementAttempts();
			//IOException Save the POD
			try 
			{

				DocumentStore.getInstance().update(pod, pod.getMessageId());
			} 
			catch (DocumentSaveFailedException e1) 
			{
				//Log
				NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
						.messageId(String.valueOf(pod.getMessageId()))
						.action("Pod updation failed")
						.description(pod.getMessageDeliveryState() == 100 || pod.getMessageDeliveryState() == 200?"Publish":"ThreeWayAck"+"counter decrement failed after MessageBuildFailedException")
						.logError(), e1);
			}
		} 
		catch (DocumentSaveFailedException e) {
			NcephLogger.MONITOR_LOGGER.warn(new MonitorLog.Builder()
					.monitorPort(connector.getPort())
					.action("File read attribute failed")
					.description("Cannot read attributes of file "+messageId+" due to IOException")
					.logError(),e);
		} 

	}
}
