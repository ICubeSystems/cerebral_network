package com.ics.synapse.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;
import com.ics.synapse.worker.SynapticReader;

/**
 * Connector implementation for the Micro-service/ application node.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 14-Jan-2022
 */
public class SynapticConnector extends Connector
{
	Configuration config;
	
	private String cerebrumHostPath;
	
	SynapticConnector(String hostPath, Integer port, String name, WorkerPool<Reader> readerPool, WorkerPool<Writer> writerPool) 
	{
		super(port, name, readerPool, writerPool);
		this.cerebrumHostPath = hostPath;
	}
	
	@Override
	public AbstractSelectableChannel obtainSocketChannel() throws IOException 
	{
		return SocketChannel.open();
	}
	
	private void start() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException, ConnectionException
	{
		// Create live connections/ sockets as per the set minConnections
		for (int i = 0; i < config.minConnections; i++)
			connect();
	}
	
	/**
	 * This method connects the SYNAPTIC node to the CEREBRAL node. 
	 * 
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 * @throws ConnectionException 
	 * @return void
	 */
	public void connect() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException, ConnectionException
	{
		if (getActiveConnections().size() >= config.maxConnections)
			throw new ConnectionException(new Exception("Maximum number of connections reached - " + config.maxConnections));
			
		// 1. Increment the totalConnectionsServed by 1
		setTotalConnectionsServed(getTotalConnectionsServed() + 1);
		
		// 2. Create a new connection builder object
		Connection connection = new Connection.Builder()
				.id(getTotalConnectionsServed())
				.connector(this)
				.cerebralConnectorAddress(new InetSocketAddress(cerebrumHostPath, getPort())) // Connection is for SYNAPTIC connector, hence the address and port number of the CEREBRAL connector
				.build();
		// TODO - Pick the cerebral hoststring and port number from the configuration local to the installed synapse
		
		// 3. Add the connection object to load balancer for read/ write allocations
		connection.addToLoadBalancer();
		getActiveConnections().put(connection.getId(), connection);
	}
	
	@Override
	public void acceptConnection() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException 
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void createPostReadWorker(Message message, Connection incomingConnection) 
	{
		getReaderPool().execute(new SynapticReader(incomingConnection, message));
	}
	
	public Configuration getConfig() {
		return config;
	}

	private void setConfig(Configuration config) {
		this.config = config;
	}
	
	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 14-Jan-2022
	 */
	public static class Builder
	{
		private String hostPath;
		/**
		 * Port number of the cerebral connector
		 */
		private Integer port = null;
		
		/**
		 * Maximum number of sockets allowed in the pool
		 */
		private Integer maxConnections = 50;
		
		/**
		 * Number of sockets to start at the pool creation time
		 */
		private Integer minConnections = 5;
		
		/**
		 * Maximum number of active allocation after which a new socket is opened till maxPoolSize is reached.
		 */
		private Integer maxConcurrentRequest = 100;
		
		/**
		 * Maximum idle time of the socket after which it will be closed
		 */
		private int maxConnectionIdleTime = 510000; //8.5 mins
		
		private String name;
		
		private WorkerPool<Reader> readerPool;
		
		private WorkerPool<Writer> writerPool;
		
		
		/**
		 * Not required if building a SYNAPTIC connector. Port number of the server socket within the connector
		 * 
		 * @param port
		 * @return Builder
		 */
		public Builder port(Integer port) {
			this.port = port;
			return this;
		}
		
		/**
		 * Name of the connector
		 * 
		 * @param name
		 * @return Builder
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}
		
		/**
		 * Name of the connector
		 * 
		 * @param name
		 * @return Builder
		 */
		public Builder hostPath(String hostPath) {
			this.hostPath = hostPath;
			return this;
		}
		
		/**
		 * Pool of reader worker threads
		 * 
		 * @param readerPool
		 * @return Builder
		 */
		public Builder readerPool(WorkerPool<Reader> readerPool) {
			this.readerPool = readerPool;
			return this;
		}
		
		/**
		 * Pool of writer worker threads
		 * 
		 * @param writerPool
		 * @return Builder
		 */
		public Builder writerPool(WorkerPool<Writer> writerPool) {
			this.writerPool = writerPool;
			return this;
		}
		
		public Builder maxConnections(Integer maxConnections) {
			this.maxConnections = maxConnections;
			return this;
		}
		
		public Builder minConnections(Integer minConnections) {
			this.minConnections = minConnections;
			return this;
		}
		
		public Builder maxConcurrentRequest(Integer maxConcurrentRequest) {
			this.maxConcurrentRequest = maxConcurrentRequest;
			return this;
		}
		
		public Builder maxConnectionIdleTime(Integer maxConnectionIdleTime) {
			this.maxConnectionIdleTime = maxConnectionIdleTime;
			return this;
		}
		
		/**
		 * Builds the {@link Connector} instance
		 * 
		 * @return Connector
		 * @throws IOException
		 * @throws ReactorNotAvailableException 
		 * @throws ImproperReactorClusterInstantiationException 
		 * @throws ConnectionException 
		 */
		public SynapticConnector build() 
		{
			// 1. Instantiate new SynapticConnector
			SynapticConnector connnector = new SynapticConnector(
					hostPath,
					port, 
					name, 
					readerPool, 
					writerPool
					);
			// 2. Set the configurations for the connector
			connnector.setConfig(connnector.new Configuration(
					maxConnections, 
					minConnections, 
					maxConcurrentRequest, 
					maxConnectionIdleTime));
			// 3. Start the connector
			try 
			{
				connnector.start();
			} 
			catch (IOException | ImproperReactorClusterInstantiationException | ReactorNotAvailableException | ConnectionException e) 
			{
				System.out.println("ERROR: Problem in connecting to cerebral connector. Stack trace:");
				e.printStackTrace();
			}
			// 4. Initialize the monitor thread
			connnector.initializeMonitor(new SynapticMonitor(), 60, 60);
			// 5. Return the connector
			return connnector;
		}
	}

	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 17-Jan-2022
	 */
	public class Configuration
	{
		/**
		 * Maximum number of sockets allowed in the pool
		 */
		Integer maxConnections;
		
		/**
		 * Number of sockets to start at the pool creation time
		 */
		Integer minConnections;
		
		/**
		 * Maximum number of active allocation after which a new socket is opened till maxPoolSize is reached.
		 */
		Integer maxConcurrentRequest;
		
		/**
		 * Maximum idle time of the socket after which it will be closed
		 */
		Integer maxConnectionIdleTime; //8.5 mins
		
		
		public Configuration(Integer maxConnections, Integer minConnections, Integer maxConcurrentRequest, Integer maxConnectionIdleTime) 
		{
			this.maxConnections = maxConnections;
			this.minConnections = minConnections;
			this.maxConcurrentRequest = maxConcurrentRequest;
			this.maxConnectionIdleTime = maxConnectionIdleTime;
		}
	}
}
