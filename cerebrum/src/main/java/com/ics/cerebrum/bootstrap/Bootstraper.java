package com.ics.cerebrum.bootstrap;

import java.io.IOException;
import java.util.Map.Entry;

import com.ics.cerebrum.connector.CerebralConnector;
import com.ics.cerebrum.message.type.CerebralIncomingMessageType;
import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;

/**
 * 
 * @author Anurag Arya
 * @since 13 Dec, 202
 */
public class Bootstraper 
{
	ConnectorCluster connectorCluster;
	
	ReactorCluster reactorCluster;
	
	/**
	 * Constructor used by the <b>Spring container</b> to create a {@link Bootstraper} object. This object is managed by the <b>Spring container</b> and is singleton scoped. 
	 * This object is then injected into the Encephelon application via the Spring container.
	 * 
	 * @param connectorCluster
	 * @param reactorCluster
	 */
	public Bootstraper(ConnectorCluster connectorCluster, ReactorCluster reactorCluster)
	{
		// 1. Get all the Connector instances (the serverSocketChannels are not yet registered with any selector) (singleton scoped)
		this.connectorCluster = connectorCluster;
		// 2. Get the ReactorCluster (singleton scoped)
		this.reactorCluster = reactorCluster;
		// 3. Generate subscription meta data
	}
	
	/**
	 * Boots the Encephelon server
	 * 
	 * @throws IOException
	 * @return void
	 * @throws ReactorNotAvailableException
	 * @throws ImproperReactorClusterInstantiationException
	 */
	public void boot() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException
	{
		NcephLogger.BOOTSTRAP_LOGGER.info("# Connectors: " + ConnectorCluster.activeConnectors.size());
		NcephLogger.BOOTSTRAP_LOGGER.info("# Reactors: " + ReactorCluster.activeReactors.size());
		DocumentStore.initiate();
		NcephLogger.BOOTSTRAP_LOGGER.info("Initializing " + CerebralIncomingMessageType.types.length + " incoming message types");
		NcephLogger.BOOTSTRAP_LOGGER.info("Initializing " + CerebralOutgoingMessageType.types.length + " outgoing message types");
		
		// 3. Loop over connectorCluster and register selector
		for (Entry<Integer, Connector> entry : ConnectorCluster.activeConnectors.entrySet())
		{
			CerebralConnector connector = (CerebralConnector)entry.getValue();
			connector.assignReactor(ReactorCluster.getReactor());
		}
		
		
		
		// 4. Run the reactors
		reactorCluster.run();
	}
}
