package com.ics.nceph.core.connector.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ics.logger.ConnectionLog;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.MessageReader;
import com.ics.nceph.core.message.MessageWriter;
import com.ics.nceph.core.message.RelayFailedMessageHandlingThread;
import com.ics.nceph.core.message.exception.RelayTimeoutException;
import com.ics.nceph.core.message.type.MessageType;
import com.ics.nceph.core.reactor.Reactor;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.ssl.exception.SSLHandshakeException;


/**
 * This class encapsulates a connection between Service/ Application and a particular Encephelon {@link Connector}
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public class Connection implements Comparable<Connection> 
{
	private Integer id;
	
	private SocketChannel socket;
	
	private Reactor reactor;
	
	private Connector connector;
	
	private SelectionKey key;
	
	private int relayTimeout;
	
	Metric metric;
	
	/**
	 * Queue of messages to be relayed to the subscriber nodes.
	 */
	private ConcurrentLinkedQueue<Message> relayQueue;
	
	private MessageReader reader;
	
	private MessageWriter writer;
	
	private ConnectionState state;
	
	long lastUsed;
	
	boolean isClient;
	
	int plainTextBufferSize = NcephConstants.READER_BUFFER_SIZE;

	/**
	 * 
	 * @return
	 */
	public ConcurrentLinkedQueue<Message> getRelayQueue() {
		return relayQueue;
	}
	
	/**
	 * Constructs a connection for cerebral connector
	 * 
	 * @param id
	 * @param connector
	 * @param relayTimeout
	 * @param receiveBufferSize
	 * @param sendBufferSize
	 * @throws IOException
	 * @throws ConnectionException 
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 */
	Connection(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize) throws ConnectionInitializationException, ConnectionException
	{
		this.relayTimeout = relayTimeout;
		// 1. Get the SocketChannel and accept the incoming connection
		try 
		{
			this.socket = ((ServerSocketChannel)connector.obtainSocketChannel()).accept();
		}
		catch (IOException e) {	throw new ConnectionException("Connection with id:"+ id +" constructor failed: "+e.getMessage(), e);}
		
		// 2. Initialize the connection created above
		initialize(id, connector, relayTimeout, receiveBufferSize, sendBufferSize, false);
	}
	
	/**
	 * Constructs a connection for synaptic connector
	 * 
	 * @param id
	 * @param connector
	 * @param relayTimeout
	 * @param receiveBufferSize
	 * @param sendBufferSize
	 * @param cerebralConnectorAddress
	 * @throws ConnectionException 
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 */
	Connection(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize, InetSocketAddress cerebralConnectorAddress) throws ConnectionInitializationException, ConnectionException
	{
		// 1. Obtain IO channel & connect
		try 
		{
			this.socket = (SocketChannel)connector.obtainSocketChannel();
			// Connect to the cerebral server
			this.socket.connect(cerebralConnectorAddress);
		} catch (IOException e) {
			// TODO throw connectionException
			throw new ConnectionException("Connection with id:"+ id +" constructor failed: "+e.getMessage(), e);
		}
		this.relayTimeout = relayTimeout;
		// 2. Initialize connection
		initialize(id, connector, relayTimeout, receiveBufferSize, sendBufferSize, true);
	}
	
	
	private void initialize(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize, boolean isClient) throws ConnectionInitializationException
	{
		try 
		{
			this.id = id;
			this.connector = connector;
			
			// Initialize the counters to 0
			this.metric = new Metric();
			
			// Get the reactor from the connector which has least number of active keys 
			this.reactor = ReactorCluster.getReactor();
			this.socket.socket().setSendBufferSize(sendBufferSize);
			this.socket.socket().setReceiveBufferSize(receiveBufferSize);
			
			// Set the socketChannel to nonblocking mode
			this.socket.configureBlocking(false);
			// Set the client mode to true, indicating that the connection is on the client side (synaptic node).
			this.isClient = isClient;
			// Initialize the connection
			initializeConnection();
		} catch (Exception e) 
		{
			try 
			{
				teardown();
			} catch (IOException teardownException) {
				throw new ConnectionInitializationException("Teardown failed while initalizing connection", teardownException);
			}
			throw new ConnectionInitializationException("Connection initialization failed", e);
		}
	}
	
	protected void initializeConnection() throws IOException, SSLHandshakeException 
	{
		// Connection state is AUTH_PENDING when constructed - can only be used for event read and relay after the state changes to READY
		state = ConnectionState.AUTH_PENDING;
		// If the handshake is successful then register a selector to socket channel with interestOps
		key = getSocket().register(reactor.getSelector(), SelectionKey.OP_READ, this);
		// Initialize the MessageReader & MessageWriter
		reader = new MessageReader(this);
		writer = new MessageWriter(this);
		reactor.getSelector().wakeup();
		
		// Initialize the eventQueue
		relayQueue = new ConcurrentLinkedQueue<Message>();
		// set last used time of the connection
		setLastUsed(System.currentTimeMillis());
	}

	
	/**
	 * This method should be called when the underlying socket gives a <b>java.net.SocketException: Connection reset"</b> exception. 
	 * The most common cause of SocketException is writing or reading data to or from a closed socket connection. The client application might have terminated or closed the socket.
	 * 
	 * The method will remove the {@link Connection} from the Connector's load balancer, ensuring that no further relay is done on this connection. 
	 * The message queued for relay are then transferred to relay queue of the connector. They will be assigned connection for relay once new connections are accepted on the connector. 
	 * Finally the Selection Key is cancelled and the socket are closed if they are still open.
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @return void
	 * @throws IOException 
	 * @since 09-Jan-2022
	 */
	public void teardown() throws IOException
	{
		// LOG: Connection (id:2): Connection teardown. Transfer X messages to transmissionQueue of Connector (port: 1000)
		NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
				.connectionId(String.valueOf(id))
				.action("teardown initiated")
				.logInfo());
		state = ConnectionState.TEARDOWN_REQUESTED;
		
		// Remove the connection from LB to re-adjust the counters
		removeFromLoadBalancer();
		
		// Check if there are any messages waiting to be relayed. Transfer them to the connectors outgoing buffer
		if (relayQueue != null && relayQueue.size() > 0)
		{
			NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
					.connectionId(String.valueOf(getId()))
					.action("Teardown - dump")
					.description("Transfering messages to connector's relayQueue")
					.data(new LogData()
							.entry("port", String.valueOf(getConnector().getPort()))
							.entry("#messages", String.valueOf(relayQueue.size()))
							.toString())
					.logInfo());
			while(!relayQueue.isEmpty())
			{
				Message message = relayQueue.poll();
				getConnector().removeConnectionQueuedUpMessage(message);
				getConnector().enqueueMessage(message);
			}
		}
		
		// Cancel the selection key
		key.cancel();
		
		// Close the socket if it is not already closed
		if (!getSocket().socket().isClosed())
			getSocket().socket().close();
		
		// Close the channel if it is not already closed
		if (getSocket().isOpen())
			getSocket().close();
		
		// Remove the connection from the map of active connections on the connector & change the state of the connection to DECOMMISIONED
		getConnector().getActiveConnections().remove(getId());
		state = ConnectionState.DECOMMISIONED;
		NcephLogger.CONNECTION_LOGGER.info(new ConnectionLog.Builder()
				.connectionId(String.valueOf(id))
				.action("teardown completed")
				.data(new LogData()
						.entry("ConnectionId", String.valueOf(id))
						.toString())
				.logInfo());
	}
	
	/**
	 * Read the event from the {@link SocketChannel} instance assigned to this {@link Connection} instance. 
	 * 
	 * @return void
	 * @throws IOException 
	 * @throws ConnectionInitializationException 
	 */
	public void read() throws IOException 
	{
		boolean operationStatus = true;

		// 1. Engage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB
		engage();

		// 2. Invoke the MessageReader to read the message(s) from the underlying socket
		try 
		{
			reader.read();
		}catch (IOException e) // In case there is an exception white reading from socket
		{
			// TODO maybe we want to handle this by creating our custom exception - TBD
			//Check if the SocketException () then initiate teardown()
			if(e instanceof SocketException || e instanceof ClosedChannelException)
			{
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getId()))
						.action("Teardown")
						.description("Teardown connection due to: ")
						.logError(),e);
				teardown();
			}
			else
				operationStatus = false;
		}

		// 3. Disengage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB
		disengage(operationStatus, false);
	}

	/**
	 * 
	 * @param operationStatus
	 * @return void
	 */
	public void disengage(boolean operationStatus, boolean temporaryWriteDisabled) 
	{
		synchronized (getConnector().getConnectionLoadBalancer()) 
		{
			if(isReady())
			{
				// Update the last used of the connection
				setLastUsed(System.currentTimeMillis());

				// Remove the connection from LB to re-adjust the counters
				removeFromLoadBalancer();

				// Decrement the activeRequests counter
				getMetric().activeRequests.decrementAndGet();

				// If read/ write was without any error/ exception then increment the totalSuccessfulRequestsServed counter
				if (operationStatus)
					getMetric().totalSuccessfulRequestsServed.incrementAndGet();

				// Add the connection to the LB after counters are re-adjusted and if the write operation is not disabled on the connection due to relayTimeout
				if (isReady() && !temporaryWriteDisabled)
					addToLoadBalancer();
			}
		}
	}

	/**
	 * 
	 * @return void
	 */
	public void engage() 
	{
		synchronized (getConnector().getConnectionLoadBalancer()) 
		{
			if(isReady())
			{
				// Update the last used of the connection
				setLastUsed(System.currentTimeMillis());

				// Remove the connection from LB to re-adjust the counters
				removeFromLoadBalancer();

				// Increment the counters
				getMetric().activeRequests.incrementAndGet();
				getMetric().totalRequestsServed.incrementAndGet();

				// Add the connection to the LB after counters are re-adjusted 
				addToLoadBalancer();
			}
		}
	}

	/**
	 * Write/ publish the events enqueued in the event queue of the connection to the socket channel
	 * 
	 * @return void
	 * @throws IOException 
	 */
	public void write() throws IOException
	{
		boolean operationStatus = true;
		// 1. Write only if the relayQueue has any message to write
		if (relayQueue.size() > 0)
		{
			// 2. Loop over the relayQueue till it is empty
			try 
			{
				while(!relayQueue.isEmpty())
				{
					// 2.1 Engage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB
					
					engage();
					// 2.2 Relay the message
					writer.write(relayQueue.peek());
					// 2.3 Update the last used of the connection
					setLastUsed(System.currentTimeMillis());
					// 3. Disengage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB if the relayTimeout has not occurred
					disengage(operationStatus, writer.isReady() ? false : true);
				}
			}
			// In case there is an IO exception while writing to the socket
			catch (IOException e) 
			{
				// TODO Update the flag of the message which failed
				// TODO maybe we want to handle this by creating our custom exception - TBD
				// Check if the SocketException then initiate teardown()
				if(e instanceof SocketException || e instanceof ClosedChannelException)
				{
					teardown();
					state = ConnectionState.TEARDOWN_REQUESTED;
				}
				else
					operationStatus = false;
				disengage(operationStatus, writer.isReady() ? false : true);
			}
			// The client/ receiver end of the socket is not able to accept the bytes within the set relayTimeout. 
			// Then the RelayTimeoutException will be thrown and it will breaks the above write loop (stopping further relay of messages). 
			catch (RelayTimeoutException e) 
			{
				//Create a new thread (RelayFailedMessageHandlingThread) which will re attempt the write in some time
				new RelayFailedMessageHandlingThread.Builder()
				.connection(this)
				.waitBeforeWriteAgain(60000*5) // 5 Minutes wait before retrying the write operation
				.build()
				.start(); // Start the thread
				disengage(operationStatus, writer.isReady() ? false : true);
			}
		}

		// 4. Set the connnection's socket channel interest to read
		setInterest(SelectionKey.OP_READ);
	}
	
	/**
	 * Makes the connection's {@link SelectionKey} interested in write operation and notifies the selector to wake up in next select cycle
	 * 
	 * @return void
	 */
	public synchronized void setInterest(int selectionInterest) 
	{
		key.interestOps(selectionInterest);
		key.selector().wakeup();
	}
	
	public synchronized void removeFromLoadBalancer()
	{
		getConnector().getConnectionLoadBalancer().remove(this);
	}
	
	public synchronized void addToLoadBalancer()
	{
		getConnector().getConnectionLoadBalancer().add(this);
	}
	
	@Override
	public int compareTo(Connection connection) 
	{
		if(getMetric().activeRequests.get() > connection.getMetric().activeRequests.get() // if active request is greater then return 1
				|| (getMetric().activeRequests.get() == connection.getMetric().activeRequests.get() && relayQueue.size() > connection.relayQueue.size()) // if active request is same and relayQueue size is greater then return 1
				|| (getMetric().activeRequests.get() == connection.getMetric().activeRequests.get() && relayQueue.size() == connection.relayQueue.size() && getMetric().totalRequestsServed.get() > connection.getMetric().totalRequestsServed.get())) // if active request is same and totalRequestsServed is greater then return 1 
		{
			return 1;
		} else if (getMetric().activeRequests.get() < connection.getMetric().activeRequests.get() 
				|| (getMetric().activeRequests.get() == connection.getMetric().activeRequests.get() && relayQueue.size() < connection.relayQueue.size())
				|| (getMetric().activeRequests.get() == connection.getMetric().activeRequests.get() && relayQueue.size() == connection.relayQueue.size() && getMetric().totalRequestsServed.get() < connection.getMetric().totalRequestsServed.get())) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() 
	{
		return "Connection {id=" + id + 
				", activeRequests=" + getMetric().activeRequests +
				", RequestsServed=" + getMetric().totalRequestsServed +
				", SuccessfulRequestsServed=" + getMetric().totalSuccessfulRequestsServed +
				", state=" + state +
				", relayQueueSize=" + relayQueue.size() +
				", WriterReady=" + writer.isReady() +
				", IdleTime=" + getIdleTime() +
				'}';
	}

    @Override
    public boolean equals(Object o) 
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection connection = (Connection) o;
        return Integer.compare(connection.getMetric().activeRequests.get(), getMetric().activeRequests.get()) == 0 &&
        		Integer.compare(connection.id, id) == 0 &&
        		Integer.compare(connection.getMetric().totalRequestsServed.get(), getMetric().totalRequestsServed.get()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, getMetric().activeRequests, getMetric().totalRequestsServed);
    }
    
	/**
	 * Adds the message in the relay queue of this connection to be written over the socket channel. <br>
	 * This method checks for duplicacy of the message. 
	 * Only <b>exception</b> is when the message is being enqueued from the <code>{@link ConnectorMonitorThread}</code> thread. 
	 * Assumption is that the monitor thread resends the message only after checking the state of POD/POR on expiry of transmission window.
	 * 
	 * 
	 * @param message - message to be published
	 * @param context - QueuingContext
	 * @return void
	 */
	public void enqueueMessage(Message message, QueuingContext context)
	{
		// DUPLICACY CHECK: Check if the message has already been sent.  
		if ((message.decoder().getType() == 0x0B || message.decoder().getType() == 0x03) // Message type should be PUBLISH_EVENT or RELAY_EVENT, only then check for duplicacy
				&& (context.duplicacyCheckEnabled() && getConnector().isAlreadySent(message) // Check if the message has already been sent. If the message is being queued by the monitor then do not check for duplicacy.
				|| getConnector().isAlreadyQueuedUpOnConnection(message) || getConnector().isAlreadyQueuedUpOnConnector(message))) // Check if the message is not already in the relay queue of the connector or any of its connections
			return;
		// store message to connectionQueuedUpMessageRegister 
		getConnector().storeConnectionQueuedUpMessage(message);
		// add the message to the relay queue
		relayQueue.add(message);
		//LOG
		NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
				.messageId(message.decoder().getId())
				.action(context.logAction)
				.data(
						new LogData()
						.entry("port", String.valueOf(connector.getPort()))
						.entry("connectionId", String.valueOf(getId()))
						.entry("CallerClass", Thread.currentThread().getStackTrace()[2].getFileName())
						.entry("messageType", MessageType.getNameByType(message.decoder().getType()))
						.toString())
				.logInfo());
	}
	
	public boolean isReady()
	{
		return this.state.getValue() == ConnectionState.READY.getValue() ? true : false;
	}
	
	public long getIdleTime()
	{
		return System.currentTimeMillis() - lastUsed;
	}
	
	public void setLastUsed(long lastUsed) {
		this.lastUsed = lastUsed;
	}
	
	public Integer getId() {
		return id;
	}
	
	public SocketChannel getSocket() {
		return socket;
	}
	
	public Reactor getReactor() {
		return reactor;
	}
	
	public Connector getConnector() {
		return connector;
	}
	
	public int getRelayTimeout() {
		return relayTimeout;
	}
	
	public int getPlainTextBufferSize() {
		return plainTextBufferSize;
	}

	public ConnectionState getState()
	{
		return state;
	}
	
	public void setState(ConnectionState state) {
		this.state = state;
	}
	
	public Metric getMetric() {
		return metric;
	}
	
	public int updateMetric(Message message)
	{
		String callerContext = Thread.currentThread().getStackTrace()[2].getFileName();
		
		if (message.decoder().getType() == 0x0B || message.decoder().getType() == 0x03) // RELAY_EVENT  || PUBLISH_EVENT
			return "MessageReader.java".equals(callerContext) ? getMetric().incomingEventMessageCounter.incrementAndGet() : getMetric().outgoingEventMessageCounter.incrementAndGet(); // Received (incoming) : Sent (outgoing)
		
		if (message.decoder().getType() == 0x09 || message.decoder().getType() == 0x04) // RELAYED_EVENT_ACK || NCEPH_EVENT_ACK
			return "MessageReader.java".equals(callerContext) ? getMetric().incomingMessageAckCounter.incrementAndGet() : getMetric().outgoingMessageAckCounter.incrementAndGet(); // Received (incoming) : Sent (outgoing)
		
		if (message.decoder().getType() == 0x0C || message.decoder().getType() == 0x05) // RELAY_ACK_RECEIVED || ACK_RECEIVED
			return "MessageReader.java".equals(callerContext) ? getMetric().incomingMessage3WayAckCounter.incrementAndGet() : getMetric().outgoingMessage3WayAckCounter.incrementAndGet(); // Received (incoming) : Sent (outgoing)
		
		if (message.decoder().getType() == 0x0A || message.decoder().getType() == 0x0D) // DELETE_POD || POR_DELETED
			return "MessageReader.java".equals(callerContext) ? getMetric().incomingMessageDoneCounter.incrementAndGet() : getMetric().outgoingMessageDoneCounter.incrementAndGet(); // Received (incoming) : Sent (outgoing)
		
		return -1;
	}

	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 22-Dec-2021
	 */
	public static class Builder
	{
		private Integer id;
		
		private Connector connector;
		
		/**
		 * Defaults to 1MB
		 */
		private Integer receiveBufferSize = 1024*1024;
		
		private Integer sendBufferSize = 1024*1024;
		
		/**
		 * Maximum waiting time in milliseconds before the write operation on the socket times out and breaks the socket write loop. 
		 * Defaulted to 2 minutes unless specified in the organs.xml
		 */
		private Integer relayTimeout = 60000*5;
		
		InetSocketAddress cerebralConnectorAddress;
		
		public Builder id(Integer id)
		{
			this.id = id;
			return this;
		}
		
		public Builder receiveBufferSize(Integer receiveBufferSize)
		{
			this.receiveBufferSize = receiveBufferSize;
			return this;
		}
		
		public Builder sendBufferSize(Integer sendBufferSize)
		{
			this.sendBufferSize = sendBufferSize;
			return this;
		}
		
		public Builder connector(Connector connector)
		{
			this.connector = connector;
			return this;
		}
		
		public Builder relayTimeout(Integer relayTimeout)
		{
			this.relayTimeout = relayTimeout;
			return this;
		}
		
		public Builder cerebralConnectorAddress(InetSocketAddress cerebralConnectorAddress)
		{
			this.cerebralConnectorAddress = cerebralConnectorAddress;
			return this;
		}
		
		
		public Connection build() throws ConnectionInitializationException, ConnectionException
		{
			if(NcephConstants.TLS_MODE) {
				if (cerebralConnectorAddress != null)
					return new SSLConnection(id, connector, relayTimeout, receiveBufferSize, sendBufferSize, cerebralConnectorAddress);
				return new SSLConnection(id, connector, relayTimeout, receiveBufferSize, sendBufferSize);
			}
			else {
				if (cerebralConnectorAddress != null)
					return new Connection(id, connector, relayTimeout, receiveBufferSize, sendBufferSize, cerebralConnectorAddress);
				return new Connection(id, connector, relayTimeout, receiveBufferSize, sendBufferSize);
			}
		}
	}
	
	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 26-Jul-2022
	 */
	public static class Metric
	{
		/**
		 * Total number of messages 
		 */
		AtomicInteger activeRequests;
		
		AtomicInteger totalRequestsServed;
		
		AtomicInteger totalSuccessfulRequestsServed;
		
		AtomicInteger incomingEventMessageCounter;
		
		AtomicInteger outgoingEventMessageCounter;
		
		AtomicInteger incomingMessageAckCounter;
		
		AtomicInteger outgoingMessageAckCounter;
		
		AtomicInteger incomingMessage3WayAckCounter;
		
		AtomicInteger outgoingMessage3WayAckCounter;
		
		AtomicInteger incomingMessageDoneCounter;
		
		AtomicInteger outgoingMessageDoneCounter;
		
		Metric()
		{
			this.activeRequests = new AtomicInteger(0);
			this.totalRequestsServed = new AtomicInteger(0);
			this.totalSuccessfulRequestsServed = new AtomicInteger(0);
			this.incomingEventMessageCounter = new AtomicInteger(0);
			this.outgoingEventMessageCounter = new AtomicInteger(0);
			this.incomingMessageAckCounter = new AtomicInteger(0);
			this.outgoingMessageAckCounter = new AtomicInteger(0);
			this.incomingMessage3WayAckCounter = new AtomicInteger(0);
			this.outgoingMessage3WayAckCounter = new AtomicInteger(0);
			this.incomingMessageDoneCounter = new AtomicInteger(0);
			this.outgoingMessageDoneCounter = new AtomicInteger(0);
		}

		public AtomicInteger getActiveRequests() {
			return activeRequests;
		}

		public AtomicInteger getTotalRequestsServed() {
			return totalRequestsServed;
		}

		public AtomicInteger getTotalSuccessfulRequestsServed() {
			return totalSuccessfulRequestsServed;
		}

		public AtomicInteger getIncomingEventMessageCounter() {
			return incomingEventMessageCounter;
		}

		public AtomicInteger getOutgoingEventMessageCounter() {
			return outgoingEventMessageCounter;
		}

		public AtomicInteger getIncomingMessageAckCounter() {
			return incomingMessageAckCounter;
		}

		public AtomicInteger getOutgoingMessageAckCounter() {
			return outgoingMessageAckCounter;
		}

		public AtomicInteger getIncomingMessage3WayAckCounter() {
			return incomingMessage3WayAckCounter;
		}

		public AtomicInteger getOutgoingMessage3WayAckCounter() {
			return outgoingMessage3WayAckCounter;
		}

		public AtomicInteger getIncomingMessageDoneCounter() {
			return incomingMessageDoneCounter;
		}

		public AtomicInteger getOutgoingMessageDoneCounter() {
			return outgoingMessageDoneCounter;
		}
		
		
	}
}