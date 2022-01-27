package com.ics.synapse.connector;

import java.io.IOException;
import java.util.Map.Entry;

import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;

public class SynapticMonitor extends ConnectorMonitorThread 
{
	@Override
	public void monitor() throws ImproperMonitorInstantiationException
	{
		SynapticConnector connector = (SynapticConnector) getConnector();
		// 1. Loop through all the active connections within the connector
		System.out.println("[Monitor thread] Active connections: " +  connector.getActiveConnections().size());
		for (Entry<Integer, Connection> connectionEntry : connector.getActiveConnections().entrySet()) 
		{
			Connection connection = connectionEntry.getValue();
			System.out.println("Connection ["+connection.getId() + "] has been idle for:" + connection.getIdleTime());
			// check if the connection has been idle (no read write operation) for more than connector's maxConnectionIdleTime
			if(connection.getIdleTime() > connector.config.maxConnectionIdleTime)
			{
				
				System.out.println("Connection ["+connection.getId() + "] IdleTime exceeded - Teardown Initiated");
				// Check if there are active requests in the connection. If yes then defer the teardown else proceed to teardown
				if(connection.getActiveRequests().get() <= 0)
				{
					try 
    				{
						// Initiate the teardown of connection
						connection.teardown();
    				}
					catch (Exception e) 
    				{
						System.out.println("Connection ["+connection.getId() + "] IdleTime exceeded - Teardown Failed (stack trace below):");
    					e.printStackTrace();
    				}
				}
				// Ideally the below else block should never be executed if the connector's maxConnectionIdleTime & connection's relayTimeout settings are set properly
				// Only logging such occurrence for now
				else 
				{
					System.out.println("**TBH** - IdleTime exceeded but connection has mroe than 0 activeRequests. Collecting data:");
					System.out.println(connection + " - Idle Since: " + connection.getIdleTime());
					System.out.println("**END - TBH**");
				}
			}
		}

		// 2. Create new connection if activeConnections has lesser number of connections than config.minConnections
		if (connector.getActiveConnections().size() < connector.config.minConnections) 
		{
			System.out.println("Number of active connections: " + connector.getActiveConnections().size() + " - Creating new connection...");
			try 
			{
				connector.connect();
			} catch (IOException | ImproperReactorClusterInstantiationException | ReactorNotAvailableException | ConnectionException e) {
				e.printStackTrace();
			}
		}
    }
}
