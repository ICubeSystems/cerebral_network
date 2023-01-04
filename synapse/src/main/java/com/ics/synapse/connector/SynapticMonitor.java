package com.ics.synapse.connector;

import java.util.Map.Entry;

import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MonitorLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.AuthenticationFailedException;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.connector.state.ConnectorState;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.store.ConfigStore;
import com.ics.nceph.core.db.document.store.cache.MessageCache;

/**
 * A {@link ConnectorMonitorThread Monitor} thread residing on the synaptic node, responsible for continuous monitoring the state of the messages to be published by the synapse. 
 * SynapticMonitor thread is created at the time of bootstrapping. It enforces RELIABILITY to the nceph network, i.e - <b>guaranteed delivery of all the published messages to cerebrum</b>.  
 * <br>
 * Following monitoring tasks are performed by the SynapticMonitor:
 * <ol>
 * 	<li>Create new connections if the connector has less than the configured <i>(config.minConnections)</i> number of activeConnections</li>
 * 	<li>If there are any messages in the connector's relay queue then transfer them to connection's relay queue for transmission</li>
 * 	<li>Check for PODs which have exceeded the {@link Configuration#APPLICATION_PROPERTIES transmission.window} configuration and process them as per their state:</li>
 * 		<ul>
 * 			<li>INITIAL | DELIVERED: re-send PUBLISH_EVENT message</li>
 * 			<li>ACKNOWLEDGED | ACK_RECIEVED: re-send ACK_RECIEVED message</li>
 * 			<li>FINISHED: Delete the POD from local storage</li>
 * 		</ul>
 * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Jan-2022
 */
public class SynapticMonitor extends ConnectorMonitorThread<SynapticConnector> 
{
	@Override
	public void monitorRelayQueue()
	{
		connectorRelayQueueTransfer();
	}

	@Override
	public void monitorConnections()
	{
		if(getConnector().getState() != ConnectorState.BACKPRESSURE_INITIATED) {
		// 1. Loop through all the active connections within the connector
		teardownIdleConnections();
		
		// 2. Create new connection if activeConnections has lesser number of connections than config.minConnections
		manageConnectionPool();
		}
	}
	
	@Override
	public void monitorMessages()
	{
		// Get the proof of publish message cache and process them
		MessageCache<ProofOfPublish> popCache = ProofOfPublish.getMessageCache(getConnector().getPort());
		if (popCache != null) 
			popCache.entrySet()
				.stream()
				.parallel()
				.forEach(MonitorPublish.builder()
							.connector(getConnector())
							.build());
	}

	private void teardownIdleConnections() 
	{
		NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
				.monitorPort(getConnector().getPort())
				.action("Active Connection")
				.description(String.valueOf(getConnector().getActiveConnections().size()))
				.logInfo());

		for (Entry<Integer, Connection> connectionEntry : getConnector().getActiveConnections().entrySet()) 
		{
			Connection connection = connectionEntry.getValue();
			NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
					.monitorPort(getConnector().getPort())
					.action("Idle Time")
					.description("Connection ["+connection.getId() + "] has been idle for:" + connection.getIdleTime())
					.logInfo());
			// check if the connection has been idle (no read write operation) for more than connector's maxConnectionIdleTime
			if(connection.getIdleTime() > getConnector().config.maxConnectionIdleTime)
			{
				NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
						.monitorPort(getConnector().getPort())
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
								.monitorPort(getConnector().getPort())
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
							.monitorPort(getConnector().getPort())
							.action("Connection IdleTime Exceeded")
							.data(
									new LogData()
									.entry("Connection", connection.toString()).toString())
							.description("**TBH** - IdleTime exceeded but connection has more than 0 activeRequests. Collecting data:")
							.logInfo());
				}
			}}
	}
	
	private void manageConnectionPool() 
	{
		if (getConnector().getActiveConnections().size() < getConnector().config.minConnections) 
		{
			NcephLogger.MONITOR_LOGGER.warn(new MonitorLog.Builder()
					.monitorPort(getConnector().getPort())
					.action("Creating new connection")
					.data(new LogData().entry("active connections", String.valueOf(getConnector().getActiveConnections().size())).toString())
					.description("Create new connection if active connections are less than min connection")
					.logInfo());
			int connectionDeficit =  getConnector().config.minConnections - getConnector().getActiveConnections().size();
			try 
			{
				for(int i=0; i < connectionDeficit;i++)
					getConnector().connect(ConfigStore.getInstance().getNodeId());
			} catch (ConnectionInitializationException | ConnectionException | AuthenticationFailedException e) {
				//Log
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.action("Connection Initialization failed")
						.logError(),e);
			}
		}
	}

}
