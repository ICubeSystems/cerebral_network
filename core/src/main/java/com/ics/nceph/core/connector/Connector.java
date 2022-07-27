package com.ics.nceph.core.connector;

import java.io.IOException;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.message.MasterMessageLedger;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.MessageWriter;
import com.ics.nceph.core.message.type.MessageType;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.Worker;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;

/**
 * <p><b>Encephelon Network</b> has 2 incomingMessageType of nodes:<br>
 * <ol>
 * 	<li> EventData relay server: Central node/ server which receives the events and then relays them to appropriate subscriber nodes in the network</li>
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

	private SSLContext sslContext;

	// Queue of messages which needs to be relayed by the connector
	private ConcurrentLinkedQueue<Message> relayQueue;

	/**
	 * <p>
	 * 	This register stores messageIds which are received by all the active connections in this connector.<br> 
	 *   
	 * </p>
	 */
	private MasterMessageLedger incomingMessageRegister;

	/**
	 * <p>
	 * 	This register stores messageIds which are written out by all the active connections in this connector.<br> 
	 *  Queued up messages are <b>NOT</b> in this register. <br><br>
	 *   
	 * </p>
	 */
	private MasterMessageLedger outgoingMessageRegister;

	/**
	 * <p> 
	 * 	This register stores messages which are added to the {@link Connection#relayQueue relayQueue} of all the active connections in this connector. (
	 * 	<b>Note:</b> Messages in connector's relayQueue will <b>NOT</b> be here) <br><br>
	 * 	
	 * 	Messages will be added from {@link Connection#enqueueMessage(Message) enqueueMessage} method of connection
	 * 
	 *  <br><br>
	 * 	Messages in the register will be removed from:
	 * 	<ol>
	 * 		<li>{@link MessageWriter#write(Message)}}</li>
	 * 		<li>{@link Connection#teardown()}}</li>
	 * 	</ol>
	 * </p>
	 */
	private MasterMessageLedger connectionQueuedUpMessageRegister;

	/**
	 * <p> 
	 * 	This register stores messages which are added to the {@link Connector#relayQueue relayQueue} of this connector. 
	 *  <br><b>Note:</b> Messages in connector's relayQueue will be here <br><br>
	 * 	
	 * 	Messages will be added from {@link Connector#enqueueMessage(Message) enqueueMessage} method of connector
	 * 
	 *  <br><br>
	 * 	Messages in the register will be removed from:
	 * 	<ol>
	 * 		<li>{@link Connection#enqueueMessage(Message)}}</li>
	 * 	</ol>
	 * </p>
	 */
	private MasterMessageLedger connectorQueuedUpMessageRegister;

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
	public abstract void acceptConnection() throws IOException, ConnectionInitializationException;

	/**
	 * Contact method to be implemented by the implementation classes to create the Worker ({@link Reader}) threads 
	 * 
	 * @param message
	 * @param incomingConnection
	 * @return void
	 */
	public abstract void createPostReadWorker(Message message, Connection incomingConnection);

	/**
	 * Contact method to be implemented by the implementation classes to create the Worker ({@link Reader}) threads 
	 * 
	 * @param message
	 * @param incomingConnection
	 * @return void
	 */
	public abstract void createPostWriteWorker(Message message, Connection incomingConnection);

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
			WorkerPool<Writer> writerPool,
			SSLContext sslContext) 
	{
		this.port = port;
		this.name = name;
		this.readerPool = readerPool;
		this.writerPool = writerPool;
		this.sslContext = sslContext;
		initialize();
	}

	private void initialize()
	{
		connectionLoadBalancer = new PriorityBlockingQueue<Connection>();
		incomingMessageRegister = new MasterMessageLedger();
		outgoingMessageRegister = new MasterMessageLedger();
		connectionQueuedUpMessageRegister = new MasterMessageLedger();
		connectorQueuedUpMessageRegister = new MasterMessageLedger();
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

	public MasterMessageLedger getIncomingMessageRegister() {
		return incomingMessageRegister;
	}

	public MasterMessageLedger getOutgoingMessageRegister() {
		return outgoingMessageRegister;
	}

	public MasterMessageLedger getConnectionQueuedUpMessageRegister() {
		return connectionQueuedUpMessageRegister;
	}

	public MasterMessageLedger getConnectorQueuedUpMessageRegister() {
		return connectorQueuedUpMessageRegister;
	}

	/**
	 * Stores an incoming message on this connector
	 * 
	 * @param id
	 * @param message
	 * @return void
	 */
	public synchronized void storeIncomingMessage(Message message)
	{
		incomingMessageRegister.add(message);
	}

	/**
	 * Stores an outgoing message on this connector
	 * 
	 * @param id
	 * @param message
	 * @return void
	 */
	public synchronized void storeOutgoingMessage(Message message)
	{
		outgoingMessageRegister.add(message);
	}

	/**
	 * 
	 * @param message
	 */
	public synchronized void storeConnectionQueuedUpMessage(Message message)
	{
		connectionQueuedUpMessageRegister.add(message);
	}

	/**
	 * 
	 * @param message
	 */
	public synchronized void storeConnectorQueuedUpMessage(Message message)
	{
		connectorQueuedUpMessageRegister.add(message);
	}

	/**
	 * Removes the message from connection queued up message register
	 * @param message
	 */
	public synchronized void removeConnectionQueuedUpMessage(Message message)
	{
		connectionQueuedUpMessageRegister.remove(message);
	}

	/**
	 * Removes the message from connector queued up message register
	 * @param message
	 */
	public synchronized void removeConnectorQueuedUpMessage(Message message)
	{
		connectorQueuedUpMessageRegister.remove(message);
	}

	/**
	 * This method checks for duplicacy of messages relayed
	 * 
	 * @param Message message
	 * @return
	 */
	public boolean isAlreadySent(Message message)
	{
		return outgoingMessageRegister.contains(message);
	}

	/**
	 * This method checks for duplicacy of messages queued up in connection to be relayed
	 * 
	 * @param Message message
	 * @return
	 */
	public boolean isAlreadyQueuedUpOnConnection(Message message)
	{
		return connectionQueuedUpMessageRegister.contains(message);
	}

	/**
	 * This method checks for duplicacy of messages queued up in connector to be relayed
	 * 
	 * @param Message message
	 * @return
	 */
	public boolean isAlreadyQueuedUpOnConnector(Message message)
	{
		return connectorQueuedUpMessageRegister.contains(message);
	}

	/**
	 * Adds a message to connector's relay queue in case there are no active connections in the connector
	 * 
	 * @param Message to be sent out
	 * @return void
	 */
	public synchronized void enqueueMessage(Message message)
	{
		// Check if the message has ever been sent
		if ((message.decoder().getType() == 0x0B || message.decoder().getType() == 0x03)
				&& (isAlreadySent(message) || isAlreadyQueuedUpOnConnection(message) || isAlreadyQueuedUpOnConnector(message)))
			return;

		// Store in queued up register and add to relay queue
		storeConnectorQueuedUpMessage(message);
		relayQueue.add(message);

		// LOG
		NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
				.messageId(message.decoder().getId())
				.action("Enqueued to connector")
				.description("No connections found for writing/ Teardown intitiated")
				.data(
						new LogData()
						.entry("port", String.valueOf(getPort()))
						.entry("workerClass", MessageType.getClassByType(message.decoder().getType()))
						.entry("CallerClass", Thread.currentThread().getStackTrace()[2].getFileName())
						.toString())
				.logInfo());
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

	public ConnectorType getType(){
		return type;
	}

	public ConcurrentHashMap<Integer, Connection> getActiveConnections() {
		return activeConnections;
	}

	public SSLContext getSslContext() {
		return sslContext;
	}
}
