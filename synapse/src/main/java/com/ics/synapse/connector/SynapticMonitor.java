package com.ics.synapse.connector;

import java.io.File;
import java.nio.channels.SelectionKey;
import java.util.Map.Entry;

import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.MonitorLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.connection.exception.AuthenticationFailedException;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.PodState;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.AcknowledgeMessage;
import com.ics.nceph.core.message.EventMessage;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ThreeWayAcknowledgementData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * This is a thread class which is responsible for continuous monitoring of the messages coming in from the application for relay. 
 * This class provides 100% RELIABILITY to the cerebral network, i.e - <b>guaranteed relay of all the messages to all the subscribers</b><br>
 * Monitor thread is created at the time of bootstrapping. <br>
 * 
 * Following tasks are performed by the monitor to achieve reliability:
 * <ol>
 * 	<li>Create new connection if activeConnections has lesser number of connections than config.minConnections</li>
 * 	<li>If there are any messages in the connector's relay queue then transfer them to connection's relay queue for transmission</li>
 * 	<li>Check for PODs which are not deleted for more than a specified time and process them as per their POR states:</li>
 * 		<ul>
 * 			<li>INITIAL | RELAYED: re-send RELAY_EVENT message</li>
 * 			<li>ACKNOWLEDGED | ACK_RECIEVED: re-send RELAY_ACK_RECEIVED message</li>
 * 			<li>FINISHED: Move the POD to global persistent storage (DynamoDB)</li>
 * 		</ul>
 * </ol>
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Jan-2022
 */
public class SynapticMonitor extends ConnectorMonitorThread 
{
	@Override
	public void monitor() throws ImproperMonitorInstantiationException, ImproperConnectorInstantiationException
	{
		SynapticConnector connector = (SynapticConnector) getConnector();
		// 1. Loop through all the active connections within the connector
		NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
				.monitorPort(connector.getPort())
				.action("Active Connection")
				.description(String.valueOf(connector.getActiveConnections().size()))
				.logInfo());

		for (Entry<Integer, Connection> connectionEntry : connector.getActiveConnections().entrySet()) 
		{
			Connection connection = connectionEntry.getValue();
			NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
					.monitorPort(connector.getPort())
					.action("Idle Time")
					.description("Connection ["+connection.getId() + "] has been idle for:" + connection.getIdleTime())
					.logInfo());
			// check if the connection has been idle (no read write operation) for more than connector's maxConnectionIdleTime
			if(connection.getIdleTime() > connector.config.maxConnectionIdleTime)
			{
				NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
						.monitorPort(connector.getPort())
						.action("Teardown Initiated")
						.description("Connection ["+connection.getId() + "] IdleTime (" + connection.getIdleTime() + " ms) exceeded - Teardown Initiated")
						.logInfo());
				// Check if there are active requests in the connection. If yes then defer the teardown else proceed to teardown
				if(connection.getMetric().getActiveRequests().get() <= 0)
				{
					try 
					{
						// Initiate the teardown of connection
						connection.teardown();
					}
					catch (Exception e) 
					{
						NcephLogger.MONITOR_LOGGER.warn(new MonitorLog.Builder()
								.monitorPort(connector.getPort())
								.action("Teardown Failed")
								.description("Connection ["+connection.getId() + "] IdleTime exceeded - Teardown Failed (stack trace below):")
								.logInfo());
					}
				}
				// Ideally the below else block should never be executed if the connector's maxConnectionIdleTime & connection's relayTimeout settings are set properly
				// Only logging such occurrence for now
				else 
				{
					NcephLogger.MONITOR_LOGGER.warn(new MonitorLog.Builder()
							.monitorPort(connector.getPort())
							.action("Connection IdleTime Exceeded")
							.data(
									new LogData()
									.entry("Connection", connection.toString()).toString())
							.description("**TBH** - IdleTime exceeded but connection has more than 0 activeRequests. Collecting data:")
							.logInfo());
				}
			}
		}

		// 2. Create new connection if activeConnections has lesser number of connections than config.minConnections
		if (connector.getActiveConnections().size() < connector.config.minConnections) 
		{
			NcephLogger.MONITOR_LOGGER.warn(new MonitorLog.Builder()
					.monitorPort(connector.getPort())
					.action("Creating new connection")
					.data(new LogData().entry("active connections", String.valueOf(connector.getActiveConnections().size())).toString())
					.description("Create new connection if active connections are less than min connection")
					.logInfo());
			int connectionDeficit =  connector.config.minConnections - connector.getActiveConnections().size();
			try 
			{
				for(int i=0; i < connectionDeficit;i++)
					connector.connect();
			} catch (ConnectionInitializationException | ConnectionException | AuthenticationFailedException e) {
				//Log
				e.printStackTrace();
			}
		}

		// 3. Loop through the connectors relay queue and transfer to the connections queue
		try 
		{
			while(!connector.getRelayQueue().isEmpty()) 
			{
				Connection connection = connector.getConnection();
				connection.enqueueMessage(connection.getConnector().getRelayQueue().poll(), QueuingContext.QUEUED_FROM_MONITOR);
				connection.setInterest(SelectionKey.OP_WRITE);
			}
		} catch (Exception e) {}

		// 4. Check for PODs which are not deleted for more than a specified time
		File podDirectory = new File(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location"));
		NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
				.monitorPort(connector.getPort())
				.action("Synaptic monitor")
				.description("Check uncompleted pods")
				.logInfo());

		ProcessPOD:
		{
			//4.1 get all files from the POD directory
			// 4.2 if there are no pods then exit ProcessPOD block
			if (podDirectory.listFiles() == null) 
			{
				NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
						.monitorPort(connector.getPort())
						.action("Synaptic monitor")
						.description("message dirctory empty")
						.logInfo());
				break ProcessPOD;
			}

			for (File podFile : podDirectory.listFiles()) 
			{
				ProofOfDelivery pod = null;
				int podState = 0;
				try 
				{
					// get connection from connector active connections
					Connection connection = connector.getConnection();

					// If there are no active connections in the connector then break.
					if(connection == null) 
						break;

					// check pod file is older than x minutes. 
					if (emitTransmissionWindowElapsed(podFile)) 
					{
						// load pod file
						pod = (ProofOfDelivery)DocumentStore.load(podFile, ProofOfDelivery.class);
						// get pod state of current pod
						podState = pod.getPodState().getState();

						switch (podState) 
						{
						case 100:// INITIAL state of POD
						case 200:// PUBLISHED state of POD
							// Create PUBLISH_EVENT message 
							Message message1 = new EventMessage.Builder()
							.event(pod.getEvent())
							.mid(pod.getMessageId())
							.buildAgain();

							enqueueMessage(connection, message1);
							// Set the publish attempts
							pod.incrementPublishAttempts();
							// Set POD State to PUBLISHED
							pod.setPodState(PodState.PUBLISHED);
							break;
						case 300:// ACKNOWLEDGED state of POD
						case 400:// ACK_RECIEVED state of POD
							// Create the ACK_RECEIVED message  
							Message message = new AcknowledgeMessage.Builder()
							.data(new ThreeWayAcknowledgementData.Builder()
									.writeRecord(pod.getWriteRecord()) // WriteRecord of PUBLISH_EVENT
									.ackNetworkRecord(pod.getAckNetworkRecord()) // NCEPH_EVENT_ACK network record
									.build())
							.mid(pod.getMessageId())
							.type(SynapticOutgoingMessageType.ACK_RECEIVED.getMessageType())
							.build();
							enqueueMessage(connection, message);
							// Set the threeWayAck attempts
							pod.incrementThreeWayAckAttempts();
							// Set POD State to ACK_RECIEVED
							pod.setPodState(PodState.ACK_RECIEVED);
							break;
						case 500:// FINISHED state of POD
							// Delete the POD from local storage
							if (!DocumentStore.delete(pod.getMessageId(),pod))
							{
								NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
										.messageId(pod.getMessageId())
										.action("POD deletion failed")
										.logError());
							}
							break;
						default:
							break;
						}
						DocumentStore.update(pod, podFile.getName());
					}
				} 
				catch (MessageBuildFailedException e) 
				{
					// Log
					NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
							.messageId(podFile.getName())
							.action(podState == 100 || podState == 200?"NCEPH_EVENT build failed":"ACK_RECEIVED build failed")
							.description("message build failed in monitor")
							.logError(),e);
					pod.decrementAttempts();
					//IOException Save the POD
					try 
					{
						DocumentStore.update(pod, pod.getMessageId());
					} 
					catch (DocumentSaveFailedException e1) 
					{
						//Log
						NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
								.messageId(String.valueOf(pod.getMessageId()))
								.action("Pod updation failed")
								.description(podState == 100 || podState == 200?"Publish":"ThreeWayAck"+"counter decrement failed after MessageBuildFailedException")
								.logError(), e1);
					}
				} 
				catch (DocumentSaveFailedException e) {
					NcephLogger.MONITOR_LOGGER.warn(new MonitorLog.Builder()
							.monitorPort(connector.getPort())
							.action("File read attribute failed")
							.description("Cannot read attributes of file "+podFile.getName()+" due to IOException")
							.logError(),e);
				} 
			}
			NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
					.monitorPort(connector.getPort())
					.action("Synaptic monitor")
					.description("Check uncompleted pods complete")
					.logInfo());
		}
		
		
	}
}
