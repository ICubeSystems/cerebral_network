package com.ics.synapse.bootstrap;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Value;

import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.RejectedReaderHandler;
import com.ics.nceph.core.worker.RejectedWriterHandler;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;
import com.ics.synapse.Emitter;
import com.ics.synapse.connector.SynapticConnector;

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
	
	public void boot() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException, ConnectionException, ImproperConnectorInstantiationException
	{
		System.out.println("# Reactors: " + ReactorCluster.activeReactors.size());
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
