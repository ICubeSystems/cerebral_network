package com.ics.nceph.core.connector;

import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;

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
}
