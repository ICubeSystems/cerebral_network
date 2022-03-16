package com.ics.cerebrum.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import com.ics.cerebrum.worker.CerebralReader;
import com.ics.cerebrum.worker.CerebralWriter;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.reactor.Reactor;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;

/**
 * Connector implementation for the Event relay server node.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Dec-2021
 */
public class CerebralConnector extends Connector
{
	private Integer bufferSize;
	
	private ServerSocketChannel serverChannel;
	
	CerebralConnector(
			Integer port, 
			String name, 
			Integer bufferSize, 
			WorkerPool<Reader> readerPool, 
			WorkerPool<Writer> writerPool) throws IOException 
	{
		super(port, name, readerPool, writerPool);
		this.bufferSize = bufferSize;
		initializeCerebralConnector();
	}
	
	private void initializeCerebralConnector() throws IOException
	{
		// TODO exception handling in creation of server socket - create a new exception for server socket creation
		// Open a ServerSocketChannel for communication
		serverChannel = ServerSocketChannel.open();
		// Get the ServerSocket from the ServerSocketChannel & bind it to a port to listen
		serverChannel.socket().bind(new InetSocketAddress("127.0.0.1", getPort()));
		// Set the ServerSocketChannel to nonblocking mode
		serverChannel.configureBlocking(false);
		
		// This set the ReceiveBufSize for all the SocketChannels which will be accepted in this serverChannel
		serverChannel.setOption(StandardSocketOptions.SO_RCVBUF, 1024*256);
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
		System.out.println("Assigning selector of reactor " + reactor.getReactorId() + "to port " + getPort() + " to accept new connections");
		getServerChannel().register(reactor.getSelector(), SelectionKey.OP_ACCEPT, this);
	}
	
	/**
	 * This method accepts a {@link SocketChannel} from the {@link ServerSocketChannel} when their is an incoming connection request from any service/ application on this connector
	 * 
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 22-Dec-2021
	 */
	public synchronized void acceptConnection() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException
	{
		// 1. Increment the totalConnectionsServed by 1
		setTotalConnectionsServed(getTotalConnectionsServed()+1);
		
		// 2. Create a new connection builder object
		Connection connection = new Connection.Builder()
				.id(getTotalConnectionsServed())
				.connector(this)
				.build();
		
		// 3. Add the connection object to load balancer for read/ write allocations
		getConnectionLoadBalancer().add(connection);
		getActiveConnections().put(connection.getId(), connection);
		
		// 4. If there are outgoing messages in the buffer then transfer them to the connections relayQueue
		if (getRelayQueue().size() > 0)
		{
			System.out.println("Enqueueing " + getRelayQueue().size() + " messages from the outgoing buffer (relayQueue) to connection [id:" + connection.getId() + "] relayQueue");
			while(!getRelayQueue().isEmpty())
				connection.enqueueMessage(getRelayQueue().poll());
			connection.setInterest(SelectionKey.OP_WRITE);
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
		getReaderPool().execute(new CerebralReader(incomingConnection, message));
	}
	
	@Override
	public void createPostWriteWorker(Message message, Connection incomingConnection) 
	{
		getWriterPool().execute(new CerebralWriter(incomingConnection, message));
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
		 * Builds the {@link Connector} instance
		 * 
		 * @throws IOException
		 * @return Connector
		 */
		public CerebralConnector build() throws IOException
		{
			// 1. Instantiate new CerebralConnector
			CerebralConnector connnector = new CerebralConnector(
								port, 
								name, 
								bufferSize, 
								readerPool,
								writerPool
								);
			// 2. Initialize the monitor thread
			connnector.initializeMonitor(new CerebralMonitor(), 60, 60);
			// 3. Return the connector
			return connnector;
		}
	}
}
