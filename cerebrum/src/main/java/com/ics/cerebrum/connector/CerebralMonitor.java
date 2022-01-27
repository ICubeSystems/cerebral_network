package com.ics.cerebrum.connector;

import java.nio.channels.SelectionKey;

import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.connector.exception.ImproperMonitorInstantiationException;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Jan-2022
 */
public class CerebralMonitor extends ConnectorMonitorThread 
{
	@Override
	public void monitor() throws ImproperMonitorInstantiationException, ImproperConnectorInstantiationException 
	{
		CerebralConnector connector = (CerebralConnector) getConnector();
		// Check if there are any messages in the relay queue
		if (connector.getRelayQueue().size() > 0)
		{
			// Get the connection with the least load
			Connection connection = connector.getConnection();
			
			// Check if the connection is not null
			if (connection != null)
			{
				System.out.println("Enqueueing " + connector.getRelayQueue().size() + " messages from the outgoing buffer (relayQueue) to connection [id:" + connection.getId() + "] relayQueue");
				// Transfer 20 messages at a time to the connection with the least load
				int transferCounter = 0;
				while(!connector.getRelayQueue().isEmpty() && transferCounter++ <= 20)
					connection.enqueueMessage(connector.getRelayQueue().poll());
			}
			connection.setInterest(SelectionKey.OP_WRITE);
		}
	}
}
