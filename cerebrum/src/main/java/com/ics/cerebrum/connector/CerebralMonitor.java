package com.ics.cerebrum.connector;

import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.store.cache.MessageCache;

/**
 * This is a thread class which is responsible for continuous monitoring of the messages flowing through cerebrum. 
 * This class provides 100% RELIABILITY to the cerebral network, i.e - <b>guaranteed relay of all the messages to all the subscribers</b><br>
 * One monitor thread per connecter is created at the time of cerebral bootstrapping. <br>
 * 
 * Following tasks are performed by the monitor to achieve reliability:
 * <ol>
 * 	<li>If there are any messages in the connector's relay queue then transfer them to connection's relay queue for transmission</li>
 * 	<li>Check for PODs which are not deleted for more than a specified time and process them as per their POR states:</li>
 * 		<ul>
 * 			<li>INITIAL | RELAYED: re-send RELAY_EVENT message</li>
 * 			<li>ACKNOWLEDGED | ACK_RECIEVED: re-send RELAY_ACK_RECEIVED message</li>
 * 			<li>FINISHED: Move the POD to global persistent storage (DynamoDB)</li>
 * 		</ul>
 * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 18-Jan-2022
 */
public class CerebralMonitor extends ConnectorMonitorThread<CerebralConnector>
{
	/**
	 * The DB repositories are manually injected via this constructor from the connectorCulsterInitializer. 
	 * TODO: This should be done using spring container instead.
	 */
	public CerebralMonitor() {}
	
	@Override
	public void monitorMessages()
	{
		// Get the proof of publish message cache and process them
		MessageCache<ProofOfPublish> popCache = ProofOfPublish.getMessageCache(getConnector().getPort());
		if (popCache != null) 
			popCache.entrySet()
				.stream()
				.parallel()
				.forEach(MonitorRelay.builder()
							.connector(getConnector())
							.build());
	}

	@Override
	public void monitorRelayQueue()
	{
		connectorRelayQueueTransfer();
	}

	@Override
	public void monitorConnections(){}
}