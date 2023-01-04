package com.ics.cerebrum.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;

import javax.net.ssl.SSLContext;

import com.ics.cerebrum.message.type.CerebralIncomingMessageType;
import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.cerebrum.worker.CerebralReader;
import com.ics.cerebrum.worker.CerebralWriter;
import com.ics.logger.BootstraperLog;
import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.connector.state.ConnectorState;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.PauseTransmissionMessage;
import com.ics.nceph.core.message.ResumeTransmissionMessage;
import com.ics.nceph.core.message.data.BackpressureData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.reactor.Reactor;
import com.ics.nceph.core.receptor.PauseTransmissionReceptor;
import com.ics.nceph.core.receptor.ResumeTransmissionReceptor;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;

/**
 * Connector implementation for the EventData relay server node.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Dec-2021
 */
public class CerebralConnector extends Connector
{
	private Integer bufferSize;
	
	private ServerSocketChannel serverChannel;
	
	/**
	 * Concurrent map of synaptic nodes connected to this connector and their active connections
	 */
	private ConcurrentHashMap<Integer, PriorityBlockingQueue<Connection>> nodeWiseConnections;
	
	CerebralConnector(
			Integer port, 
			String name, 
			Integer bufferSize, 
			WorkerPool<Reader> readerPool, 
			WorkerPool<Writer> writerPool,
			SSLContext sslContext) throws IOException 
	{
		super (port, name, readerPool, writerPool, sslContext);
		this.nodeWiseConnections = new ConcurrentHashMap<Integer, PriorityBlockingQueue<Connection>>();
		this.bufferSize = bufferSize;
		initializeCerebralConnector();
	}
	
	private void initializeCerebralConnector() throws IOException
	{
		// TODO exception handling in creation of server socket - create a new exception for server socket creation
		// Open a ServerSocketChannel for communication
		serverChannel = ServerSocketChannel.open();
		// Get the ServerSocket from the ServerSocketChannel & bind it to a port to listen
		serverChannel.socket().bind(new InetSocketAddress(getPort()));
		// Set the ServerSocketChannel to nonblocking mode
		serverChannel.configureBlocking(false);
		// This set the ReceiveBufSize for all the SocketChannels which will be accepted in this serverChannel
		serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024*256);
		// Set connector state to ready
		this.setState(ConnectorState.READY);
	}
	
	/**
	 * This method assigns a {@link Reactor} instance which will listen to the {@link SelectionKey#OP_ACCEPT} operation for this {@link Connector} instance. 
	 * 
	 * @param {@link Reactor} instance which will listen to the {@link SelectionKey#OP_ACCEPT} operation on this {@link Connector} instance
	 * @return void
	 * @throws ClosedChannelException 
	 */
	public void assignReactor(Reactor reactor) throws ClosedChannelException
	{
		// Register the ServerSocketChannel of the Connector with the Selector of the supplied Reactor
		NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
									.id(String.valueOf(reactor.getReactorId()))
									.action("Assigning selector")
									.data(new LogData().entry("Port", String.valueOf(getPort()))
											.toString())
									.logInfo());
		getServerChannel().register(reactor.getSelector(), SelectionKey.OP_ACCEPT, this);
	}
	
	/**
	 * This method removes the destroyed connection from nodeWiseConnections, this method will be called after teardown of the connection.
	 */
	@Override
	public void removeConnection(Connection connection)
	{
		// Get all connections for the node from the nodeWiseConnections
		PriorityBlockingQueue<Connection> connectionQueue = nodeWiseConnections.get(connection.getNodeId());
		
		if(connectionQueue != null) {
			// Remove connection
			connectionQueue.remove(connection);
			// Log
			NcephLogger.CONNECTOR_LOGGER.info(new ConnectionLog.Builder()
					.action("nodeWiseConnections: REMOVE")
					.connectionId(connection.getId().toString())
					.data(new LogData()
							.entry("nodeId", String.valueOf(connection.getNodeId()))
							.toString())
					.logInfo());
		}
	}
	
	/**
	 * This method accepts a {@link SocketChannel} from the {@link ServerSocketChannel} when their is an incoming connection request from any service/ application on this connector
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @throws ConnectionInitializationException 
	 * @throws IOException
	 * @return void
	 * @since 22-Dec-2021
	 */
	public void acceptConnection() throws IOException, ConnectionInitializationException
	{
		try 
		{
			// 1. Create a new connection builder object
			Connection connection = new Connection.Builder()
					.id(getTotalConnectionsServed())
					.connector(this)
					.build();
			
			// 2. Increment the totalConnectionsServed by 1
			setTotalConnectionsServed(getTotalConnectionsServed()+1);
			
			NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
					.connectionId(String.valueOf(connection.getId()))
					.action("initialise connection")
					.data(new LogData()
							.entry("state", String.valueOf(connection.getState().getValue()))
							.entry("Port", String.valueOf(connection.getConnector().getPort()))
							.toString())
					.logInfo());
		} catch (ConnectionException e) {
			NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
					.connectionId("New")
					.action("Connection build failed")
					.data(new LogData()
							.entry("Port", String.valueOf(this.getPort()))
							.toString())
					.logError(),e);
		}
	}
	
	@Override
	public AbstractSelectableChannel obtainSocketChannel() 
	{
		return getServerChannel();
	}
	
	@Override
	public void createPostReadWorker(Message message, Connection incomingConnection) 
	{
		getReaderPool().register(new CerebralReader(incomingConnection, message));
	}
	
	@Override
	public void createPostWriteWorker(Message message, Connection incomingConnection) 
	{
		getReaderPool().execute(new CerebralWriter(incomingConnection, message));
	}
	
	/**
	 * Get the {@link ServerSocketChannel} of the connector 
	 * 
	 * @return ServerSocketChannel
	 */
	public ServerSocketChannel getServerChannel() {
		return serverChannel;
	}
	
	/**
	 * Get the buffer size of the {@link ServerSocketChannel}
	 * 
	 * @return Integer
	 */
	public Integer getBufferSize() {
		return bufferSize;
	}
	
	/**
	 * Get set of connections in node  
	 */
	public ConcurrentHashMap<Integer, PriorityBlockingQueue<Connection>> getNodeWiseConnectionsMap() {
		return nodeWiseConnections;
	}

	/**
	 * This method is called from {@link PauseTransmissionReceptor} when the synaptic sends a backpressure: {@link CerebralIncomingMessageType#PAUSE_TRANSMISSION PAUSE_TRANSMISSION} message to cerebrum. 
	 * Cerebrum removes all the active connections of that node from its load balance, this will pause any further transmission to that node. 
	 */
	@Override
	public void pauseTransmission(Integer nodeId) 
	{
		// Remove all connections of application from connectionLoadBalancer
		nodeWiseConnections.get(nodeId).forEach(conn->{
			getConnectionLoadBalancer().remove(conn);
		});
	}

	/**
	 * This method is called from {@link ResumeTransmissionReceptor} when the synaptic sends a backpressure: {@link CerebralIncomingMessageType#RESUME_TRANSMISSION RESUME_TRANSMISSION} message to cerebrum. 
	 * Cerebrum add all the active connections of that node to its load balance, this will resume further transmission to that node. All the queued up messages (if any) will be sent now.
	 */
	@Override
	public void resumeTransmission(Integer nodeId) 
	{
		// Add all connections of application to connectionLoadBalancer
		nodeWiseConnections.get(nodeId).forEach(conn->{
			getConnectionLoadBalancer().add(conn);
		});
	}
	
	/**
	 * Signal all synaptic nodes belonging to this application (port) to pause the transmission of messages by sending {@link CerebralOutgoingMessageType#PAUSE_TRANSMISSION PAUSE_TRANSMISSION} message.
	 */
	@Override
	public void signalPauseTransmission(Connection connection) 
	{
		// TODO Auto-generated method stub
		for(PriorityBlockingQueue<Connection> connectionQueue : getNodeWiseConnectionsMap().values()) 
		{
			BackpressureData data = new BackpressureData.Builder().build();
			Connection nodeConnection = connectionQueue.peek();
			try 
			{
				// Send stop message to producer
				PauseTransmissionMessage message = new PauseTransmissionMessage.Builder()
														.data(data)
														.sourceId(nodeConnection.getNodeId())
														.originatingPort(nodeConnection.getConnector().getPort())
														.build();
				nodeConnection.enqueueMessage(message, QueuingContext.QUEUED_FROM_CONNECTOR);
				nodeConnection.setInterest(SelectionKey.OP_WRITE);
			} catch (MessageBuildFailedException e) {
				// Log
				NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
						.messageId(connection.getNodeId()+"-0")
						.action("PAUSE_TRANSMISSION build failed")
						.logError(),e);
			}
		}
	}
	
	/**
	 * Signal Synapse to resume the transmission of messages by sending {@link CerebralOutgoingMessageType#RESUME_TRANSMISSION RESUME_TRANSMISSION} message.
	 */
	@Override
	public void signalResumeTransmission(Connection connection) 
	{
		for(PriorityBlockingQueue<Connection> connectionQueue : getNodeWiseConnectionsMap().values()) 
		{
			BackpressureData data = new BackpressureData.Builder().build();
			Connection nodeConnection = connectionQueue.peek();
			try {
				// Send stop message to producer
				ResumeTransmissionMessage message = new ResumeTransmissionMessage.Builder()
														.data(data)
														.sourceId(nodeConnection.getNodeId())
														.originatingPort(nodeConnection.getConnector().getPort())
														.build();
				nodeConnection.enqueueMessage(message, QueuingContext.QUEUED_FROM_CONNECTOR);
				nodeConnection.setInterest(SelectionKey.OP_WRITE);
			} catch (MessageBuildFailedException e) {
				// Log
				NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
						.messageId(connection.getNodeId()+"-0")
						.action("RESUME_TRANSMISSION build failed")
						.logError(),e);
			}
		}
	}
	
	/**
	 * Builder inner class for {@link Connector}
	 *  
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 18-Dec-2021
	 */
	public static class Builder
	{
		private Integer port = null;
		
		private String name;
		
		private Integer bufferSize;
		
		private WorkerPool<Reader> readerPool;
		
		private WorkerPool<Writer> writerPool;
		
		private SSLContext sslContext;
		
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
		 * 
		 * 
		 * @param bufferSize
		 * @return Builder
		 */
		public Builder bufferSize(Integer bufferSize) {
			this.bufferSize = bufferSize;
			return this;
		}
		
		/**
		 * Pool of reader worker threads
		 * 
		 * @param workerPool
		 * @return Builder
		 */
		public Builder readerPool(WorkerPool<Reader> workerPool) {
			this.readerPool = workerPool;
			return this;
		}
		
		/**
		 * Pool of writer worker threads
		 * 
		 * @param workerPool
		 * @return Builder
		 */
		public Builder writerPool(WorkerPool<Writer> workerPool) {
			this.writerPool = workerPool;
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
		
		/**
		 * Builds the {@link Connector} instance
		 * 
		 * @throws IOException
		 * @return Connector
		 */
		public CerebralConnector build() throws IOException
		{
			// 1. Instantiate new CerebralConnector
			CerebralConnector connector = new CerebralConnector(
								port, 
								name, 
								bufferSize, 
								readerPool,
								writerPool,
								sslContext
								);
			// 2. Initialize the monitor thread
			CerebralMonitor monitor = new CerebralMonitor();
			monitor.attachConnector(connector);
			connector.initializeMonitor(monitor, NcephConstants.MONITOR_INTERVAL, NcephConstants.MONITOR_INTERVAL);
			// 3. Return the connector
			return connector;
		}
	}

	

}
