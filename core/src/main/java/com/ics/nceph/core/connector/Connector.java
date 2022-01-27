package com.ics.nceph.core.connector;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.Worker;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;

/**
 * <p><b>Encephelon Network</b> has 2 types of nodes:<br>
 * <ol>
 * 	<li> Event relay server: Central node/ server which receives the events and then relays them to appropriate subscriber nodes in the network</li>
 * 	<li> Micro-service/ application node: The events on the application occur on these nodes, and then these nodes publish these events to the network</li>
 * </ol><br>
 * 
 * This is the base class representing <b>Connection Point<b> in a node. This class is responsible for <br>
 * <ol>
 * 	<li> Managing pool of connections</li>
 * 	<li> Accepting & creating new connections</li>
 *  <li> Managing (also balancing) the relay load amongst the connections</li>
 *  <li> Managing incoming and outgoing {@link Message} registers</li>
 *  <li> Manage {@link Worker} threads</li>
 * </ol><br>
 * 
 * The class has 2 implementation classes:
 * <ol>
 * 	<li> CerebralConnector: for event relay server</li>
 * 	<li> SynapticConnector: Micro-service/ application node</li>
 * </ol><br></p>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Dec-2021
 */
public abstract class Connector
{
	private static final Logger logger = LogManager.getLogger("nceph-core-logger");
	
	private ConnectorType type;
	
	private Integer port;
	
	private String name;
	
	private Integer totalConnectionsServed = 0;
	
	private WorkerPool<Reader> readerPool;
	
	private WorkerPool<Writer> writerPool;
	
	// Queue of messages which needs to be relayed by the connector
	private ConcurrentLinkedQueue<Message> relayQueue;
	
	// TODO size of the map (number of messages) should be fixed basis the bytes. TBD - initial considerations (128 MB). Overflows needs to be thought out. DB flushing needs to be done for the fully acknowledged message
	private ConcurrentHashMap<Long, Message> incomingMessageRegister;
	
	// TODO size of the map (number of messages) should be fixed basis the bytes. TBD - initial considerations (256 MB). Overflows needs to be thought out. 
	private ConcurrentHashMap<Long, Message> outgoingMessageRegister;
	
	/**
	 * Map of active connections in the pool
	 */
	ConcurrentHashMap<Integer, Connection> activeConnections;
	
	/**
	 * Min heap to do load balancing of {@link Connection} instances within the {@link Connector}
	 */
	PriorityBlockingQueue<Connection> connectionLoadBalancer;
	
	/**
	 * Executor to run a periodic thread to manage socket pool
	 */
	private ScheduledExecutorService monitorService;

	/**
	 * Contact method to be implemented by the implementation classes to return the socket channel during the creation of the connection
	 * 
	 * @return AbstractSelectableChannel
	 */
	public abstract AbstractSelectableChannel obtainSocketChannel() throws IOException;
	
	/**
	 * Contact method to be implemented by the implementation classes to accept the socket connection
	 * 
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 * @return void
	 */
	public abstract void acceptConnection() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException;
	
	/**
	 * Contact method to be implemented by the implementation classes to create the Worker ({@link Reader}) threads 
	 * 
	 * @param message
	 * @param incomingConnection
	 * @return void
	 */
	public abstract void createPostReadWorker(Message message, Connection incomingConnection);
	
	/**
	 * Constructor used to construct base connector
	 * 
	 * @param port
	 * @param name
	 * @param readerPool
	 * @param writerPool
	 * @throws IOException
	 */
	public Connector(
			Integer port,
			String name, 
			WorkerPool<Reader> readerPool, 
			WorkerPool<Writer> writerPool) 
	{
		this.port = port;
		this.name = name;
		this.readerPool = readerPool;
		this.writerPool = writerPool;
		
		initialize();
	}
	
	private void initialize()
	{
		connectionLoadBalancer = new PriorityBlockingQueue<Connection>();
		incomingMessageRegister = new ConcurrentHashMap<Long, Message>();
		outgoingMessageRegister = new ConcurrentHashMap<Long, Message>();
		relayQueue = new ConcurrentLinkedQueue<Message>();
		activeConnections = new ConcurrentHashMap<Integer, Connection>();
	}
	
	/**
	 * This method initializes the monitor executor service. It will only initialize if it is not initialized
	 * 
	 * @param monitor
	 * @param initialDelay
	 * @param delay
	 * @return void
	 */
	public final void initializeMonitor(ConnectorMonitorThread monitor, long initialDelay, long delay)
	{
		// If the service is not configured then initialize
		if (monitorService == null)
		{
			monitor.attachConnector(this);
			monitorService = Executors.newSingleThreadScheduledExecutor();
			monitorService.scheduleWithFixedDelay(monitor, initialDelay, delay, TimeUnit.SECONDS);
		}
		// If the service is configured and this method is called then just log the illegal usage of this method
		else
		{
			System.out.println("Illegal call of initializeMonitor method - please investigate the usage. Stack trace below:");
			new Exception("Illegal call of initializeMonitor method").printStackTrace();
		}
	}
	
	/**
	 * Get the port number of the connector
	 * 
	 * @return Integer
	 */
	public Integer getPort() {
		return port;
	}

	/**
	 * Get the name of the connector
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * This method returns the {@link Connection} instance with the least number of activeRequests
	 * 
	 * @throws ImproperConnectorInstantiationException
	 * @return Connection
	 */
	public Connection getConnection() throws ImproperConnectorInstantiationException
	{
		// 1. Check if the connectionLoadBalancer has been properly initialized
		if (connectionLoadBalancer == null)
			throw new ImproperConnectorInstantiationException(new Exception("Connector not initialized properly"), logger);  
		
		synchronized (connectionLoadBalancer) 
		{
			// 2. Peek connectionLoadBalancer to get the connection with least number of active requests (connection.activeRequests)
			return connectionLoadBalancer.peek();
		}
	}
	
	/**
	 * Stores an incoming message on this connector. The messages once fully acknowledged will be transfered to the DB storage via DBStorageThread (TBD).
	 * 
	 * @param id
	 * @param message
	 * @return void
	 */
	public synchronized void storeIncomingMessage(Message message)
	{
		incomingMessageRegister.put(message.decoder().getId(), message);
	}
	
	/**
	 * Stores an outgoing message on this connector. The messages once fully acknowledged will be transfered to the DB storage via DBStorageThread (TBD).
	 * 
	 * @param id
	 * @param message
	 * @return void
	 */
	public synchronized void storeOutgoingMessage(Message message)
	{
		outgoingMessageRegister.put(message.decoder().getId(), message);
	}
	
	/**
	 * Adds a message to the outgoingBuffer in case there are no active connections in the connector
	 * 
	 * @param Message to be sent out
	 * @return void
	 */
	public synchronized void enqueueMessage(Message message)
	{
		relayQueue.add(message);
	}
	
	public Integer getTotalConnectionsServed() {
		return totalConnectionsServed;
	}
	
	public void setTotalConnectionsServed(Integer totalConnectionsServed) {
		this.totalConnectionsServed = totalConnectionsServed;
	}

	public ConcurrentLinkedQueue<Message> getRelayQueue() {
		return relayQueue;
	}
	
	public WorkerPool<Reader> getReaderPool() {
		return readerPool;
	}

	public WorkerPool<Writer> getWriterPool() {
		return writerPool;
	}
	
	public PriorityBlockingQueue<Connection> getConnectionLoadBalancer() {
		return connectionLoadBalancer;
	}

	public ConnectorType getType() {
		return type;
	}
	
	public ConcurrentHashMap<Integer, Connection> getActiveConnections() {
		return activeConnections;
	}
}
