package com.ics.cerebrum.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.RejectedReaderHandler;
import com.ics.nceph.core.worker.RejectedWriterHandler;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;

/**
 * Factory class to create {@link ConnectorCluster} from the organs.xml.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Dec-2021
 */
public class ConnectorClusterInitializer 
{
	ReactorCluster reactorCluster;
	
	/**
	 * Map of EventId -> ArrayList of connectors subscribed for the event
	 */
	HashMap<Integer, ArrayList<Connector>> subscriptions;
	
	public ConnectorClusterInitializer(ReactorCluster reactorCluster)
	{
		this.reactorCluster = reactorCluster;
		this.subscriptions = new HashMap<Integer, ArrayList<Connector>>();
	}
	
	public ConnectorCluster initializeConnectionCluster() throws IOException
	{
		ConnectorCluster connectorCluster = new ConnectorCluster();
		
		// @todo: read organs.xml
		// Loop over the organs and instantiate Connector per organ
		
		// 1. Initialize a Connector
		System.out.println("Creating Connector 1 on port "+ 1000);
		CerebralConnector connector = new CerebralConnector.Builder()
				.port(1000) // BAD CODE - will be replaced by XML
				.name("test") // BAD CODE - will be replaced by XML
				.readerPool(new WorkerPool.Builder<Reader>()
						.corePoolSize(10)
						.maximumPoolSize(100)
						.keepAliveTime(60)
						.workQueue(new LinkedBlockingQueue<Runnable>())
						.rejectedThreadHandler(new RejectedReaderHandler())
						.build())
				.writerPool(new WorkerPool.Builder<Writer>()
						.corePoolSize(10)
						.maximumPoolSize(100)
						.keepAliveTime(60)
						.workQueue(new LinkedBlockingQueue<Runnable>())
						.rejectedThreadHandler(new RejectedWriterHandler())
						.build())
				.build();
		
		// 2. Add the newly created connector to the ConnectorCluster
		connectorCluster.add(connector);
		
		
		System.out.println("Creating Connector 2 on port "+ 1001);
		// 1. Initialize a Connector
		CerebralConnector connector1 = new CerebralConnector.Builder()
				.port(1001) // BAD CODE - will be replaced by XML
				.name("test1") // BAD CODE - will be replaced by XML
				.readerPool(new WorkerPool.Builder<Reader>()
						.corePoolSize(10)
						.maximumPoolSize(100)
						.keepAliveTime(60)
						.workQueue(new LinkedBlockingQueue<Runnable>())
						.rejectedThreadHandler(new RejectedReaderHandler())
						.build())
				.writerPool(new WorkerPool.Builder<Writer>()
						.corePoolSize(10)
						.maximumPoolSize(100)
						.keepAliveTime(60)
						.workQueue(new LinkedBlockingQueue<Runnable>())
						.rejectedThreadHandler(new RejectedWriterHandler())
						.build())
				.build();
		
		// 2. Add the newly created connector to the ConnectorCluster
		connectorCluster.add(connector1);
		
		// @TODO: Subscription meta creation from subscription.xml.
		subscribeForEvent(1000, 1001);
		ConnectorCluster.subscriptions = subscriptions;
		
		return connectorCluster;
	}
	
	private void subscribeForEvent(Integer eventId, Integer port) 
	{
		ArrayList<Connector> connectors = subscriptions.get(eventId);
		if (connectors == null)
			connectors = new ArrayList<Connector>();
		connectors.add(ConnectorCluster.getConnector(port));
		subscriptions.put(eventId, connectors);
	}
}
