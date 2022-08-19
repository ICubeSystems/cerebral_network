package com.ics.nceph.core.connector;

import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ics.nceph.core.event.exception.EventNotSubscribedException;

/**
 * Cluster of all the {@link Connector} instances in the Encephelon server.
 * 
 * <p>The number of {@link Connector} instances in the cluster is determined by the number of services/ applications registered in organs.xml.
 * ConnectorCluster is created by a factory class {@link ConnectorClusterInitializer} during the boot process of the Encephelon server. </p>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Dec-2021
 */
public class ConnectorCluster
{
	private static final Logger logger = LogManager.getLogger("nceph-core-logger");
	
	/**
	 * Map of active {@link Connector} instances in the Encephelon server
	 */
	public static HashMap<Integer, Connector> activeConnectors;
	
	/**
	 * Map of eventId and array of subscriber connector ports
	 */
	public static HashMap<Integer, ArrayList<Connector>> subscriptions;
	
	/**
	 * Map of event id and application receptor of connector ports
	 */
	public static HashMap<Integer, HashMap<Integer, String>> applicationReceptors;
	
	/**
	 * Default constructor initializing the maps declared above
	 */
	public ConnectorCluster()
	{
		activeConnectors = new HashMap<Integer, Connector>();
	}
	
	/**
	 * Synchronized method to add an active server to the cluster
	 * 
	 * @param Connector - connector to add to the cluster
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @throws ClosedChannelException 
	 * @since 18-Dec-2021
	 */
	public synchronized void add(Connector connector) throws ClosedChannelException
	{
		// put the connector to the cluster
		activeConnectors.put(connector.getPort(), connector);
	}
	
	/**
	 * Synchronized method to remove an active {@link Connector} from the cluster
	 * 
	 * @param connector
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 18-Dec-2021
	 */
	public synchronized void remove(Connector connector)
	{
		activeConnectors.remove(connector.getPort());
	}
	
	/**
	 * Get the ArrayList<Connector> of connectors which have subscribed for the event
	 * 
	 * @param eventId
	 * @throws EventNotSubscribedException
	 * @return ArrayList<Connector> subscriber connectors
	 */
	public synchronized static ArrayList<Connector> getSubscribedConnectors(Integer eventId) throws EventNotSubscribedException
	{
		ArrayList<Connector> subscribers = subscriptions.get(eventId);
		if (subscribers == null)
			throw new EventNotSubscribedException(new Exception("No subscribers available for eventId: "+eventId), logger);
		
		return subscribers;
	}
	
	/**
	 * Synchronized method to retrieve an active {@link Connector} by port number
	 * 
	 * @param port
	 * @return NcephConnector
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 20-Dec-2021
	 */
	public synchronized static Connector getConnector(Integer port)
	{
		return activeConnectors.get(port);
	}
	
}
