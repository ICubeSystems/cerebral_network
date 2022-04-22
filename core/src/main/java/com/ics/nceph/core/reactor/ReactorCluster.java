package com.ics.nceph.core.reactor;

import java.nio.channels.SelectionKey;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.PriorityBlockingQueue;

import com.ics.logger.NcephLogger;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;

/**
 * Cluster of all the running {@link Reactor} instances in the Encephelon
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 20-Dec-2021
 */
public class ReactorCluster
{
	/**
	 * Map of active/ running {@link Reactor} instances in the Encephelon server
	 */
	public static HashMap<Integer, Reactor> activeReactors;
	
	/**
	 * Min heap to do load balancing of {@link Reactor} instances within the {@link ReactorCluster}
	 */
	static PriorityBlockingQueue<ReactorLoad> reactorLoadBalancer;
	
	public ReactorCluster()
	{
		// Initialize the priority queue for load balancing (min heap implementation)
		reactorLoadBalancer = new PriorityBlockingQueue<ReactorLoad>();
		activeReactors = new HashMap<Integer, Reactor>();
	}
	
	/**
	 * Method to add a {@link Reactor} instance to the {@link ReactorCluster}
	 * 
	 * @param reactor
	 * @return void
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 21-Dec-2021
	 */
	public void add(Reactor reactor)
	{
		// Instantiate ReactorLoad object for the new Reactor
		ReactorLoad reactorLoad = new ReactorLoad(reactor.getReactorId(), 0);
		
		// Add the reactorLoad object to load balancer for allocation
		reactorLoadBalancer.add(reactorLoad);
		
		// Put the reactor to the cluster
		activeReactors.put(reactor.getReactorId(), reactor);
	}
	
	/**
	 * This method returns the {@link Reactor} instance with the least number of active {@link SelectionKey} instances
	 * 
	 * @return {@link Reactor} instance with the least number of active {@link SelectionKey} instances
	 * @throws ImproperReactorClusterInstantiationException if the {@link ReactorCluster#reactorLoadBalancer reactorLoadBalancer} is not initialized properly 
	 * @throws ReactorNotAvailableException if the {@link ReactorCluster#reactorLoadBalancer reactorLoadBalancer} does not return any {@link ReactorLoad reactorLoad}. 
	 * 		   This indicates that something is wrong in the min-heap implementation of the {@link ReactorCluster#reactorLoadBalancer reactorLoadBalancer}
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 21-Dec-2021
	 */
	public synchronized static Reactor getReactor() throws ImproperReactorClusterInstantiationException, ReactorNotAvailableException 
	{
		// 1. Check if the reactorLoadBalancer has been properly initialized
		if (reactorLoadBalancer == null)
			throw new ImproperReactorClusterInstantiationException(new Exception("ReactorCluster not initialized properly"));  
		
		// 2. Poll reactorLoadBalancer to get the reactor with least number of active SelectionKeys (ReactorLoad.activeKeys)
		ReactorLoad reactorLoad = reactorLoadBalancer.poll();
		
		// 3. If reactorLoadBalancer returns null reactorLoad then throw ReactorNotAvailableException
		if (reactorLoad == null)
			throw new ReactorNotAvailableException(new Exception("Reactor not available"));
		
		
		// 4. Increment the activeKeys and totalKeysServed.
		reactorLoad.setActiveKeys(reactorLoad.getActiveKeys()+1);
		reactorLoad.setTotalKeysServed(reactorLoad.getTotalKeysServed()+1);
		
		// 5 Add the reactorLoad back to reactorLoadBalancer (this will re organize the min heap)
		reactorLoadBalancer.add(reactorLoad);
		
		// 6. return the reactor with the least number of active selectionKeys
		return activeReactors.get(reactorLoad.getReactorId());
	}
	
	/**
	 * This method start the {@link Reactor} threads which are attached to any active connectors
	 * 
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 22-Dec-2021
	 */
	public void run()
	{
		NcephLogger.BOOTSTRAP_LOGGER.info("Running the reactors now");
		
		// 1. Loop over all the reactors
		for (Entry<Integer, Reactor> reactorEntry : activeReactors.entrySet()) 
		{
			Reactor reactor = reactorEntry.getValue();
			// 2. Start the reactor thread
			reactor.start();
			
		}
	}
}
