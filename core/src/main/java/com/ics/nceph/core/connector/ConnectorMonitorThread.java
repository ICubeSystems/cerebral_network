package com.ics.nceph.core.connector;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MonitorLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Jan-2022
 */
public abstract class ConnectorMonitorThread<T extends Connector> extends Thread 
{
	private T connector;

	public abstract void monitorRelayQueue();

	/**
	 * This is a contract method to be implemented by the connector monitoring threads 
	 * 
	 * @return void
	 */
	public abstract void monitorMessages();

	public abstract void monitorConnections();

	@Override
	public void run() 
	{
		if (connector == null)
		{
			// Monitor logging: Initialization failed
			return;
		}
		
		NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
				.monitorPort(getConnector().getPort())
				.action("Monitor start")
				.logInfo());
		
		// 1. Monitor connections
		monitorConnections();
		// 2. Check if there are any messages in the connector's relay queue. Transfer them to connection's relay queue for transmission.
		monitorRelayQueue();
		// 3. Monitor messages
		monitorMessages();

		NcephLogger.MONITOR_LOGGER.info(new MonitorLog.Builder()
				.monitorPort(getConnector().getPort())
				.action("Monitor end")
				.logInfo());
	}
	public void attachConnector(T connector)
	{
		if (this.connector == null)
			this.connector = connector;
	}

	public T getConnector()
	{
		return connector;
	}

	public boolean emitTransmissionWindowElapsed(File file) 
	{
		// if file is older than x minutes and whose state is not finished then resend the message to the another node to make its state to finished
		try 
		{
			if(Math.abs(System.currentTimeMillis() - Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().to(TimeUnit.MILLISECONDS)) > Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("transmission.window"))  * 1000) 
				return true;
			return false;
		}
		catch (IOException e) {
			NcephLogger.MONITOR_LOGGER.warn(new MonitorLog.Builder()
					.monitorPort(connector.getPort())
					.action("File read attribute failed")
					.description("Cannot read attributes of file "+file.getName()+" due to IOException")
					.logError(),e);
			return false;
		}
	}

	/**
	 * 
	 * Transfer messages from connector's relay queue to connection's relay queue
	 * 
	 * @throws ImproperConnectorInstantiationException 
	 * @throws ImproperMonitorInstantiationException 
	 * @since Oct 17, 2022
	 */
	public void connectorRelayQueueTransfer()
	{
		if (connector.getRelayQueue().size() > 0 && connector.getActiveConnections().size()>0)
		{
			NcephLogger.MONITOR_LOGGER.info(new ConnectionLog.Builder()
					.action("Transfer relay queue")
					.data(new LogData()
							.entry("Relay size", String.valueOf(connector.getRelayQueue().size()))
							.toString())
					.logInfo());
			Connection connection = null;
			try
			{
				while(!connector.getRelayQueue().isEmpty()) 
				{
					connection = connector.getConnection();
					Message message = connector.getRelayQueue().poll();
					connection.enqueueMessage(message, QueuingContext.QUEUED_FROM_MONITOR);
					// remove message from connectorQueuedUpMessageRegister 
					getConnector().removeConnectorQueuedUpMessage(message);
					connection.setInterest(SelectionKey.OP_WRITE);
				}
			} catch (ImproperConnectorInstantiationException e){}
		}
	}
}
