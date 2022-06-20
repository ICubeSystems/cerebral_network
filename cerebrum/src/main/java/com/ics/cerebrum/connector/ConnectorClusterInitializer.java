package com.ics.cerebrum.connector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.ics.cerebrum.nodes.xml.Subscriptions;
import com.ics.cerebrum.nodes.xml.SynapticNode;
import com.ics.cerebrum.nodes.xml.SynapticNodes;
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
 * Factory class to create {@link ConnectorCluster} from the organs.xml.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Dec-2021
 */
public class ConnectorClusterInitializer 
{
	ReactorCluster reactorCluster;

	Subscriptions eventSubscriptions;

	/**
	 * Map of EventId -> ArrayList of connectors subscribed for the event
	 */
	HashMap<Integer, ArrayList<Connector>> subscriptions;

	public ConnectorClusterInitializer(ReactorCluster reactorCluster) 
	{
		this.reactorCluster = reactorCluster;
		this.subscriptions = new HashMap<Integer, ArrayList<Connector>>();
	}

	public ConnectorCluster initializeConnectionCluster() throws IOException, JAXBException, SSLContextInitializationException
	{
		// 1. Instantitiate new ConnectorCluster
		ConnectorCluster connectorCluster = new ConnectorCluster();
		
		// 2. Load the file - SynapticNodes.xml
		InputStream xmlNode = Thread.currentThread().getContextClassLoader().getResourceAsStream("SynapticNodes.xml");

		// 3. Initialize JAXBContext for SynapticNodes object
		JAXBContext context = JAXBContext.newInstance(SynapticNodes.class);
		SynapticNodes synapticNodes = (SynapticNodes) context.createUnmarshaller().unmarshal(xmlNode); 

		// 4. Loop over synaptic nodes and create CerebralConnector per node. And create subscription meta data for the cerebrum.
		for (SynapticNode synapticNode : synapticNodes.getNodes()) 
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
			if(synapticNode.getSubscriptions()!=null) 
			{
				eventSubscriptions = synapticNode.getSubscriptions();
				for (int i = 0; i < eventSubscriptions.getEventType().size(); i++) 
					subscribeForEvent(eventSubscriptions.getEventType().get(i), synapticNode.getPort());
			}
		}

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




