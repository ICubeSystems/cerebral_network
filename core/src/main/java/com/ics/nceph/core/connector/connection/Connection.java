package com.ics.nceph.core.connector.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.MessageReader;
import com.ics.nceph.core.message.MessageWriter;
import com.ics.nceph.core.message.RelayFailedMessageHandlingThread;
import com.ics.nceph.core.message.exception.RelayTimeoutException;
import com.ics.nceph.core.reactor.Reactor;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;


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
	
	private AtomicInteger activeRequests;
	
	private AtomicInteger totalRequestsServed;
	
	private AtomicInteger totalSuccessfulRequestsServed;
	
	private Reactor reactor;
	
	private Connector connector;
	
	private SelectionKey key;
	
	private ConcurrentLinkedQueue<Message> relayQueue;
	
	private MessageReader reader;
	
	private MessageWriter writer;
	
	private ConnectionState state;
	
	long lastUsed;
	
	
	/**
	 * Constructs a connection for cerebral connector
	 * 
	 * @param id
	 * @param connector
	 * @param relayTimeout
	 * @param receiveBufferSize
	 * @param sendBufferSize
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 */
	Connection(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize) throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException
	{
		// Get the SocketChannel 
		this.socket = ((ServerSocketChannel)connector.obtainSocketChannel()).accept();
		initialize(id, connector, relayTimeout, receiveBufferSize, sendBufferSize);
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
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 */
	Connection(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize, InetSocketAddress cerebralConnectorAddress) throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException
	{
		// Get the SocketChannel 
		this.socket = (SocketChannel)connector.obtainSocketChannel();
		this.socket.connect(cerebralConnectorAddress);
		initialize(id, connector, relayTimeout, receiveBufferSize, sendBufferSize);
	}
	
	
	private void initialize(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize) throws ImproperReactorClusterInstantiationException, ReactorNotAvailableException, IOException
	{
		this.id = id;
		this.connector = connector;
		
		// Initialize the counters to 0
		this.activeRequests = new AtomicInteger(0);
		this.totalRequestsServed = new AtomicInteger(0);
		this.totalSuccessfulRequestsServed = new AtomicInteger(0);
		
		// Get the reactor from the connector which has least number of active keys 
		this.reactor = ReactorCluster.getReactor();
		
		this.socket.socket().setSendBufferSize(sendBufferSize);
		this.socket.socket().setReceiveBufferSize(receiveBufferSize);
		// Set the socketChannel to nonblocking mode
		this.socket.configureBlocking(false);
		
		// Register the selector
		key = getSocket().register(reactor.getSelector(), SelectionKey.OP_READ, this);
		reactor.getSelector().wakeup();
		
		// Initialize the MessageReader
		reader = new MessageReader();
		writer = new MessageWriter(relayTimeout);
		
		// Initialize the eventQueue
		relayQueue = new ConcurrentLinkedQueue<Message>();
		// TODO: Connection state is AUTH_PENDING when constructed - can only be used for event read and relay after the state changes to READY
		state = ConnectionState.READY;
		setLastUsed(System.currentTimeMillis());
		
		System.out.println("Initializing connection "+ id + ", attaching it to selector of reactor " + reactor.getReactorId() + " for read/ write operation");
	}
	
	/**
	 * This method should be called when the underlying socket gives a <b>java.net.SocketException: Connection reset"</b> exception. 
	 * The most common cause of SocketException is writing or reading data to or from a closed socket connection. The client application might have terminated or closed the socket.
	 * 
	 * The method will remove the {@link Connection} from the Connector's load balancer, ensuring that no further relay is done on this connection. 
	 * The message queued for relay are then transferred to relay queue of the connector. They will be assigned connection for relay once new connections are accepted on the connector. 
	 * Finally the Selection Key is cancelled and the socket are closed if they are still open.
	 * 
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @throws IOException 
	 * @since 09-Jan-2022
	 */
	public void teardown() throws IOException
	{
		// LOG: Connection (id:2): Connection teardown. Transfer X messages to transmissionQueue of Connector (port: 1000)
		System.out.println("Connection (id:"+ id +") - teardown initiated. Transfer " + relayQueue.size() + " messages to relayQueue of Connector (port: "+ getConnector().getPort() +")");
		state = ConnectionState.TEARDOWN_REQUESTED;
		
		// Remove the connection from LB to re-adjust the counters
		removeFromLoadBalancer();
		
		// Check if there are any messages waiting to be relayed. Transfer them to the connectors outgoing buffer
		if (relayQueue.size() > 0)
		{
			while(!relayQueue.isEmpty())
			{
				Message message = relayQueue.poll();
				getConnector().enqueueMessage(message);
				//LOG: Message id: 13341 [transferred]
				System.out.println("Message id: "+ message.decoder().getMessageId() + " [transferred]");
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
		
		System.out.println("Connection (id:"+ id +") - teardown completed");
	}
	
	/**
	 * Read the event from the {@link SocketChannel} instance assigned to this {@link Connection} instance. 
	 * 
	 * @return void
	 * @throws IOException 
	 */
	public void read() throws IOException 
	{
		System.out.println("READING...");
		boolean operationStatus = true;
		
		// 1. Engage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB
		engage();
		
		// 2. Invoke the MessageReader to read the message(s) from the underlying socket
		try 
		{
			reader.read(socket);
		}catch (IOException e) // In case there is an exception white reading from socket
		{
			// TODO maybe we want to handle this by creating our custom exception - TBD
			e.printStackTrace(); 
			//Check if the SocketException () then initiate teardown()
			if(e instanceof SocketException || e instanceof ClosedChannelException)
			{
				System.out.println("Initiating connection teardown due to: " + e.getMessage());
				teardown();
			}
			else
				operationStatus = false;
		}
		
		// 3. Disengage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB
		disengage(operationStatus, false);
		
		// 4. Get the fully constructed messages from the MessageReader and loop over them if more than 0
		ConcurrentHashMap<Long, Message> recievedMessages  = reader.getMessages();
		if (recievedMessages.size() > 0)
		{
			for (Entry<Long, Message> entry : recievedMessages.entrySet()) 
			{
				Long messageId = entry.getKey();
				Message message = entry.getValue();
				System.out.println("Creating reader thread for messageId:" + messageId);
				// 4.1 Create a reader thread per message
				getConnector().createPostReadWorker(message, this);
				// 4.2 Put the message in the connectors incomingMessageStore
				getConnector().storeIncomingMessage(message);
				// 4.3 Remove the message from the recievedMessages in the messageReader
				recievedMessages.remove(messageId);
			}
		}
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
			// Update the last used of the connection
			setLastUsed(System.currentTimeMillis());
						
			// Remove the connection from LB to re-adjust the counters
			removeFromLoadBalancer();
			
			// Decrement the activeRequests counter
			getActiveRequests().decrementAndGet();
			
			// If read/ write was without any error/ exception then increment the totalSuccessfulRequestsServed counter
			if (operationStatus)
				getTotalSuccessfulRequestsServed().incrementAndGet();
			
			// Add the connection to the LB after counters are re-adjusted and if the write operation is not disabled on the connection due to relayTimeout
			if (isReady() && !temporaryWriteDisabled)
				addToLoadBalancer();
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
			// Update the last used of the connection
			setLastUsed(System.currentTimeMillis());
			
			// Remove the connection from LB to re-adjust the counters
			removeFromLoadBalancer();
			
			// Increment the counters
			getActiveRequests().incrementAndGet();
			getTotalRequestsServed().incrementAndGet();
			
			// Add the connection to the LB after counters are re-adjusted 
			addToLoadBalancer();
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
		System.out.println("Writing...");
		boolean operationStatus = true;
		// 1. Write only if the relayQueue has any message to write
		if (relayQueue.size() > 0)
		{
			// 2. Engage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB
			engage();
			
			// 3. Loop over the relayQueue till it is empty
			try 
			{
				while(!relayQueue.isEmpty())
				{
					//
					// Relay the message
					writer.write(getSocket(), relayQueue.peek());
					// Update the last used of the connection
					setLastUsed(System.currentTimeMillis());
					// Check if the writer has sent the above message fully and is ready for new message
					if (writer.isReady())
						// Remove the message from the relayQueue & Store message sent to the outgoing message register
						getConnector().storeOutgoingMessage(relayQueue.poll());
				}
			}
			// In case there is an IO exception while writing to the socket
			catch (IOException e) 
			{
				// TODO Update the flag of the message which failed
				
				// TODO maybe we want to handle this by creating our custom exception - TBD
				e.printStackTrace(); 
				
				// Check if the SocketException then initiate teardown()
				if(e instanceof SocketException || e instanceof ClosedChannelException)
				{
					teardown();
					state = ConnectionState.TEARDOWN_REQUESTED;
				}
				else
					operationStatus = false;
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
			}
			
			// 3. Disengage connection - Remove the connection from LB & re-adjust the counters, finally put it back on LB if the relayTimeout has not occurred
			disengage(operationStatus, writer.isReady() ? false : true);
			
			// 4. Set the connnection's socket channel interest to read
			setInterest(SelectionKey.OP_READ);
			
			// 5. Open a write thread to do the post writing work like updating the ACK status of the messages
			//getConnector().getWriterPool().execute(new Writer(this));
		}
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
	public int compareTo(Connection connection) {
		if(getActiveRequests().get() > connection.getActiveRequests().get() // if active request is greater then return 1
				|| (getActiveRequests().get() == connection.getActiveRequests().get() && getTotalRequestsServed().get() > connection.getTotalRequestsServed().get())) // if active request is same and totalRequestsServed is greater then return 1 
		{
			return 1;
		} else if (getActiveRequests().get() < connection.getActiveRequests().get() 
				|| (getActiveRequests().get() == connection.getActiveRequests().get() && getTotalRequestsServed().get() < connection.getTotalRequestsServed().get())) {
			return -1;
		} else {
			return 0;
		}
	}

	@Override
	public String toString() {
		return "Connection {id=" + id + 
				", activeRequests=" + activeRequests +
				", RequestsServed=" + totalRequestsServed +
				", SuccessfulRequestsServed=" + totalSuccessfulRequestsServed +
				", state=" + state +
				", relayQueueSize=" + relayQueue.size() +
				", ReadUnprocessedMessageCount=" + reader.getMessages().size() +
				", WriterReady=" + writer.isReady() +
				'}';
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection connection = (Connection) o;
        return Integer.compare(connection.activeRequests.get(), activeRequests.get()) == 0 &&
        		Integer.compare(connection.id, id) == 0 &&
        		Integer.compare(connection.totalRequestsServed.get(), totalRequestsServed.get()) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, activeRequests, totalRequestsServed);
    }
    
	/**
	 * Adds the event to the event queue to be published on this connection's socket channel
	 * 
	 * @param event to be published
	 * @return void
	 */
	public void enqueueMessage(Message message)
	{
		relayQueue.add(message);
	}
	
	/**
	 * Returns the list of events to be published on the socket channel 
	 * 
	 * @return ArrayList<Event>
	 */
	public ArrayList<Message> dequeueMessages() 
	{
		ArrayList<Message> messageToPublish = new ArrayList<Message>();
		synchronized (relayQueue) 
		{
			while (relayQueue.peek() != null)
				messageToPublish.add(relayQueue.poll());
			return messageToPublish;
		}
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
	
    public AtomicInteger getActiveRequests() {
		return activeRequests;
	}

	public AtomicInteger getTotalRequestsServed() {
		return totalRequestsServed;
	}
	
	public AtomicInteger getTotalSuccessfulRequestsServed() {
		return totalSuccessfulRequestsServed;
	}

	public Reactor getReactor() {
		return reactor;
	}
	
	public Connector getConnector() {
		return connector;
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
		private Integer relayTimeout = 60000*2;
		
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
		
		public Connection build() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException
		{
			if (cerebralConnectorAddress != null)
				return new Connection(id, connector, relayTimeout, receiveBufferSize, sendBufferSize, cerebralConnectorAddress);
			return new Connection(id, connector, relayTimeout, receiveBufferSize, sendBufferSize);
		}
	}
}
