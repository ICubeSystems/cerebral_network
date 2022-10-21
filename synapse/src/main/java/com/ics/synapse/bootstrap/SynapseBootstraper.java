package com.ics.synapse.bootstrap;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import com.ics.logger.NcephLogger;
import com.ics.menu.SynapticMenu;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.ssl.NcephSSLContext;
import com.ics.nceph.core.ssl.exception.SSLContextInitializationException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.RejectedReaderHandler;
import com.ics.nceph.core.worker.RejectedWriterHandler;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;
import com.ics.synapse.Emitter;
import com.ics.synapse.connector.SynapticConnector;
import com.ics.synapse.message.type.SynapticIncomingMessageType;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 13-Jan-2022
 */
public class SynapseBootstraper 
{
	ReactorCluster reactorCluster;
	
	private String connectorName = Configuration.APPLICATION_PROPERTIES.getConfig("synapticConnector.name");
			
	private Integer cerebrumPort = Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("cerebrum.port"));
	
	private String cerebrumHostPath = Configuration.APPLICATION_PROPERTIES.getConfig("cerebrum.host.path");;
	
	/**
	 * Constructor used by the <b>Spring container</b> to create a {@link SynapseBootstraper} object. This object is managed by the <b>Spring container</b> and is singleton scoped. 
	 * This object is then injected into the Synapse application via the Spring container.
	 * 
	 * @param reactorCluster
	 * @return 
	 */
	public SynapseBootstraper(ReactorCluster reactorCluster)
	{
		System.out.println("Bootstraping in progress .......");
		// 1. Get the SynapticReactorCluster (singleton scoped)
		this.reactorCluster = reactorCluster;
	}

	public void boot() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException, ConnectionException, ImproperConnectorInstantiationException, SSLContextInitializationException
	{
//		IdStore.getInstance().initialize();
		NcephLogger.BOOTSTRAP_LOGGER.info("Initializing " + SynapticIncomingMessageType.types.length + " incoming message types");
		NcephLogger.BOOTSTRAP_LOGGER.info("Initializing " + SynapticOutgoingMessageType.types.length + " outgoing message types");

		// 1. Create a synaptic connector
		SynapticConnector connector = new SynapticConnector.Builder()
				.name(connectorName) 
				.port(cerebrumPort) // Should pick from local project configuration where the synapse is installed
				.hostPath(cerebrumHostPath)
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
				.sslContext(NcephSSLContext.getSSLContext())
				.build();
		// Add the connector to the cluster. On the synapse there will only be 1 connector in the cluster. Added for consistency.
		ConnectorCluster connectorCluster = new ConnectorCluster();
		connectorCluster.add(connector);
		
		// 2. Instantiate the singleton Emitter object
		Emitter.initiate(connector);
		
		// 3. Run the reactors
		reactorCluster.run();
		
		// 4. synaptic menu
		SynapticMenu.run(connector);
		
		
		while (connector.getActiveConnections().size()==0)
		{
			System.out.println("Waiting for connection establishment...");
			try
			{
				Thread.sleep(2000);
			} catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Builder class of bootstraper
	 */
	public static class Builder
	{
		ReactorCluster reactorCluster;

		public Builder ReactorCluster(ReactorCluster reactorCluster)
		{
			this.reactorCluster = reactorCluster;
			return this;
		}

		public SynapseBootstraper build()
		{
			return new SynapseBootstraper(reactorCluster);
		}
	}
}
