package com.ics.cerebrum.connector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Autowired;

import com.ics.cerebrum.configuration.CerebralConfiguration;
import com.ics.cerebrum.configuration.exception.ConfigurationException;
import com.ics.cerebrum.db.document.NetworkConfiguration;
import com.ics.cerebrum.db.document.Subscription;
import com.ics.cerebrum.db.document.SynapticNodesList;
import com.ics.logger.BootstraperLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.ssl.NcephSSLContext;
import com.ics.nceph.core.ssl.exception.SSLContextInitializationException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.RejectedReaderHandler;
import com.ics.nceph.core.worker.RejectedWriterHandler;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;

/**
 * Factory class to create {@link ConnectorCluster}.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Dec-2021
 */
public class ConnectorClusterInitializer 
{
	ReactorCluster reactorCluster;

	List<Subscription> eventSubscriptions;
	
	@Autowired
	private CerebralConfiguration cerebralConfiguration;
	
	/**
	 * Map of EventId -> ArrayList of connectors subscribed for the event
	 */
	HashMap<Integer, ArrayList<Connector>> subscriptions;
	
	HashMap<Integer, HashMap<Integer, String>> applicationReceptors;

	public ConnectorClusterInitializer(ReactorCluster reactorCluster) 
	{
		this.reactorCluster = reactorCluster;
		this.subscriptions = new HashMap<Integer, ArrayList<Connector>>();
		this.applicationReceptors = new HashMap<Integer, HashMap<Integer, String>>();
	}

	public ConnectorCluster initializeConnectionCluster() throws IOException, ConfigurationException, SSLContextInitializationException
	{
		// 1. Instantitiate new ConnectorCluster
		ConnectorCluster connectorCluster = new ConnectorCluster();
		
		SynapticNodesList synapticNodes;
		
		synapticNodes = cerebralConfiguration.getSynapticNodes();
		
		
		// 4. Loop over synaptic nodes and create CerebralConnector per node. And create subscription meta data for the cerebrum.
		for (NetworkConfiguration synapticNode : synapticNodes) 
		{
			NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
					.action("Creating Connector")
					.description("Creating Connector "+synapticNode.getName()+" on port " + synapticNode.getPort())
					.logInfo());
			
			// 4.1 Create CerebralConnector per node
			CerebralConnector connector = new CerebralConnector.Builder()
					.port(synapticNode.getPort())
					.name(synapticNode.getName())
					.readerPool(new WorkerPool.Builder<Reader>()
							.corePoolSize(synapticNode.getReaderPool().getCorePoolSize())
							.maximumPoolSize(synapticNode.getReaderPool().getMaximumPoolSize())
							.keepAliveTime(synapticNode.getReaderPool().getKeepAliveTime())
							.workQueue(new LinkedBlockingQueue<Runnable>())
							.rejectedThreadHandler(new RejectedReaderHandler()).build())
					.writerPool(new WorkerPool.Builder<Writer>()
							.corePoolSize(synapticNode.getWriterPool().getCorePoolSize())
							.maximumPoolSize(synapticNode.getWriterPool().getMaximumPoolSize())
							.keepAliveTime(synapticNode.getWriterPool().getKeepAliveTime())
							.workQueue(new LinkedBlockingQueue<Runnable>())
							.rejectedThreadHandler(new RejectedWriterHandler()).build())
					.sslContext(NcephSSLContext.getSSLContext())
					.build();

			// 4.2. Add the newly created connector to the ConnectorCluster
			connectorCluster.add(connector);
			
			// 4.3. Loop over the subscriptions & create subscription meta data for the cerebrum
			if(synapticNode.getSubscriptions() != null) 
			{
				eventSubscriptions = synapticNode.getSubscriptions();
				for (int i = 0; i < eventSubscriptions.size(); i++) 
				{
					subscribeForEvent(eventSubscriptions.get(i).getEventType(), synapticNode.getPort());
					applicationReceptorForPort(synapticNode.getPort(), eventSubscriptions.get(i));
				}
			}
			
		}

		ConnectorCluster.subscriptions = subscriptions;
		ConnectorCluster.applicationReceptors = applicationReceptors;
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
	
	private void applicationReceptorForPort(Integer port, Subscription subscription) 
	{
		HashMap<Integer, String> eventMap = applicationReceptors.get(port);
		if(eventMap == null)
		{
			eventMap = new HashMap<Integer,String>();
			applicationReceptors.put(port, eventMap);
		}
		eventMap.put(subscription.getEventType(), subscription.getApplicationReceptor());
	}
}