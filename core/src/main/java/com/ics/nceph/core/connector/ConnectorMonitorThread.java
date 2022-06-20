package com.ics.nceph.core.connector;

import java.io.File;
import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

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
public abstract class ConnectorMonitorThread extends Thread 
{
	Connector connector;
	
	/**
	 * This is a contract method to be implemented by the connector monitoring threads 
	 * 
	 * @return void
	 */
	public abstract void monitor() throws ImproperMonitorInstantiationException, ImproperConnectorInstantiationException;
	
	@Override
	public void run() 
	{
		try 
		{
			monitor();
		} catch (ImproperMonitorInstantiationException | ImproperConnectorInstantiationException e) 
		{
			e.printStackTrace();
		}
	}
	
	public void attachConnector(Connector connector)
	{
		if (this.connector == null)
			this.connector = connector;
	}
	
	public Connector getConnector() throws ImproperMonitorInstantiationException 
	{
		if (connector == null)
			throw new ImproperMonitorInstantiationException(new Exception("ERROR: Connector not attached"));
		return connector;
	}
	
	public boolean transmissionWindowElapsed(File file) 
	{
		// if file is older than x minutes and whose state is not finished then resend the message to the another node to make its state to finished
				try {
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
	
	public void enqueueMessage(Connection connection, Message message) 
	{
		// 1. Enqueue the message on the connection to be sent to the Cerebrum
		connection.enqueueMessage(message, QueuingContext.QUEUED_FROM_MONITOR);
		// 2. Change the interest of the connection to write
		connection.setInterest(SelectionKey.OP_WRITE);
	}
}
