package com.ics.synapse.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.Date;
import javax.net.ssl.SSLContext;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.AuthenticationFailedException;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfAuthentication;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.message.StartupMessage;
import com.ics.nceph.core.message.data.StartupData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;
import com.ics.synapse.worker.SynapticReader;
import com.ics.synapse.worker.SynapticWriter;

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

	SynapticConnector(String hostPath, Integer port, String name, WorkerPool<Reader> readerPool, WorkerPool<Writer> writerPool, SSLContext sslContext) 
	{
		super(port, name, readerPool, writerPool, sslContext);
		this.cerebrumHostPath = hostPath;
	}

	@Override
	public AbstractSelectableChannel obtainSocketChannel() throws IOException 
	{
		return SocketChannel.open();
	}

	private void start() 
	{
		// 1. Create live connections/ sockets as per the set minConnections
		for (int i = 0; i < config.minConnections; i++)
		{
			// 2. Handle IOException and proceed further
			try {
				connect();
			} catch (IOException | ConnectionInitializationException | ConnectionException | AuthenticationFailedException e) 
			{
				// 3. Log
				NcephLogger.CONNECTION_LOGGER.fatal(new ConnectionLog.Builder()
						.action("Connection failed")
						.logError(),e);
			}
		}
	}

	/**
	 * This method connects the SYNAPTIC node to the CEREBRAL node. 
	 * 
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 * @throws ConnectionException 
	 * @return void
	 * @throws AuthenticationFailedException 
	 */
	public void connect() throws IOException, ConnectionInitializationException, ConnectionException, AuthenticationFailedException
	{
		if (getActiveConnections().size() >= config.maxConnections)
			throw new ConnectionException(new Exception("Maximum number of connections reached - " + config.maxConnections));
		//TODO if connection build failed update connector's TotalConnectionsServed
		// 1. Increment the totalConnectionsServed by 1
		setTotalConnectionsServed(getTotalConnectionsServed() + 1);
		
		// 2. Create a new connection builder object
		Connection connection = new Connection.Builder()
				.id(getTotalConnectionsServed())
				.connector(this)
				.cerebralConnectorAddress(new InetSocketAddress(cerebrumHostPath, getPort())) // Connection is for SYNAPTIC connector, hence the address and port number of the CEREBRAL connector
				.build();
		// Log
		NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
				.connectionId(String.valueOf(connection.getId()))
				.action("initialize connection")
				.data(new LogData()
						.entry("state", String.valueOf(connection.getState().getValue()))
						.entry("Port", String.valueOf(connection.getConnector().getPort()))
						.toString())
				.logInfo());

		// 3. Call initiateAuthentication method to initiate authenticate connection state
		try {
			initiateAuthentication(connection);
		} catch (DocumentSaveFailedException | MessageBuildFailedException e) {
			// log
			NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
					.connectionId(String.valueOf(connection.getId()))
					.action("requesting teardown")
					.data(new LogData()
							.entry("Reason ", e.getMessage())
							.toString())
					.logInfo());
			// Connection teardown
			connection.teardown();
			throw new AuthenticationFailedException("Connection authentioction failed exception", e);
		}

	}
	
	/**
	 * 
	 * This method authenticate the connection
	 * Send Startup message to cerebrum
	 * @throws MessageBuildFailedException 
	 * @throws DocumentSaveFailedException 
	 */
	public void initiateAuthentication(Connection connection) throws MessageBuildFailedException, DocumentSaveFailedException
	{
		// 1. Create the STARTUP event 
		StartupData startupData = new StartupData.Builder()
				.startupNetworkRecord(new Date())// TODO: (to be removed by Anshul after my checkin) //startup network record with just the start
				.build();
		
		// 2. Create the STARTUP message 
		Message startupMessage = new StartupMessage.Builder().data(startupData).build();
		
		// 3. Create a ProofOfAuthentication object and save it to the local DocumentStore.
		ProofOfAuthentication poa = new ProofOfAuthentication.Builder()
				.messageId(startupMessage.decoder().getId()) // 3.1 Set message Id
				.createdOn(startupData.getCreatedOn()) // 3.2 Set createdOn
				.build();
		// 3.3 Set STARTUP network record 
		// TODO: (to be removed by Anshul after my checkin)
		poa.setStartupNetworkRecord(new NetworkRecord.Builder()
				.start(startupData.getStartupNetworkRecord())
				.build());
		// 3.4 Save the POA in the local DocumentStore
		DocumentStore.save(poa, ProofOfAuthentication.DOC_PREFIX + startupMessage.decoder().getId());
		
		// 4. Enqueue the message on the connection to be sent to the Cerebrum
		connection.enqueueMessage(startupMessage);
		// 4.1 Set the interest of the connection to write
		connection.setInterest(SelectionKey.OP_WRITE);
		
	}

	@Override
	public void acceptConnection() throws IOException, ConnectionInitializationException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void createPostReadWorker(Message message, Connection incomingConnection) 
	{
		getReaderPool().execute(new SynapticReader(incomingConnection, message));
	}

	@Override
	public void createPostWriteWorker(Message message, Connection incomingConnection) 
	{
		getWriterPool().execute(new SynapticWriter(incomingConnection, message));
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

		private SSLContext sslContext;

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

		/**
		 * Set SSLContext 
		 * 
		 * @param sslContext
		 * @return Builder
		 */
		public Builder sslContext(SSLContext sslContext) {
			this.sslContext = sslContext;
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
					writerPool,
					sslContext
					);
			
			// 2. Set the configurations for the connector
			connnector.setConfig(connnector.new Configuration(
					maxConnections, 
					minConnections, 
					maxConcurrentRequest, 
					maxConnectionIdleTime));
			
			// 3. Start the connector
			connnector.start();
			
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
