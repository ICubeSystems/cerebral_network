package com.ics.synapse.bootstrap;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Value;

import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.document.DocumentStore;
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
	
	@Value("${synapticConnector.name}")
	private String connectorName;
			
	@Value("${cerebrum.port}")
	private Integer cerebrumPort;
	
	@Value("${cerebrum.host.path}")
	private String cerebrumHostPath;
	
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
		DocumentStore.initiate();
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
		
		
		// 2. Instantiate the singleton Emitter object
		Emitter.initiate(connector);
		
		
		
		// 3. Run the reactors
		reactorCluster.run();
		/*
		System.out.println("-----------------------");
		connector.getConnection().teardown();
		System.out.println("-----------------------");
		
		connector.connect();*/
	}
}
