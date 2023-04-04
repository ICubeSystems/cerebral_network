package com.ics.cerebrum.db.store.cache;

import java.util.ArrayList;
import java.util.Map;

import com.ics.logger.BootstraperLog;
import com.ics.logger.LogData;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.db.document.ProofOfDelivery;
import com.ics.nceph.core.db.document.exception.CacheInitializationException;
import com.ics.nceph.core.db.document.store.cache.DocumentCache;
import com.ics.nceph.core.db.repository.PublishedMessageRepository;
import com.ics.nceph.core.db.repository.ReceivedMessageRepository;
import com.ics.nceph.core.message.MasterMessageLedger;
import com.ics.util.ApplicationContextUtils;

/**
 * This class is used to initialize {@link DocumentCache} and {@link MasterMessageLedger} from cloud database
 * @author Anshul
 * @version 1.0
 * @since Sep 27, 2022
 */
class DynamoDBCacheInitializer extends CerebrumCacheInitializer
{	
	private Integer port;
	
	private long start;
	
	DynamoDBCacheInitializer() throws CacheInitializationException
	{
		initialize();
	}

	void initialize() throws CacheInitializationException 
	{
		System.out.println("Generating Cache");
		if (CerebrumCacheInitializer.intializer != null)
			throw new CacheInitializationException("Cache already intialized");
		for (Map.Entry<Integer, Connector> entry : ConnectorCluster.activeConnectors.entrySet())
		{
			port = entry.getKey();
			start = System.currentTimeMillis();
			generateTransitMessageCache(ApplicationContextUtils.context.getBean("publishedMessageRepository", PublishedMessageRepository.class).findAllByActionAndMessageDeliveryStateLessThan("P:"+entry.getKey(),600), "Published Cache");
			start = System.currentTimeMillis();
			generateTransitMessageCache(ApplicationContextUtils.context.getBean("receivedMessageRepository", ReceivedMessageRepository.class).findAllByActionAndMessageDeliveryStateLessThan("R:"+entry.getKey(), 500), "Relayed Cache");
			start = System.currentTimeMillis();
			fillMasterLedger(ApplicationContextUtils.context.getBean("publishedMessageRepository", PublishedMessageRepository.class).findAllByPartitionKey("P:"+entry.getKey()), entry.getValue().getIncomingMessageRegister(), "Incoming message ledger");
			start = System.currentTimeMillis();
			fillMasterLedger(ApplicationContextUtils.context.getBean("receivedMessageRepository", ReceivedMessageRepository.class).findAllByPartitionKey("R:"+entry.getKey()), entry.getValue().getOutgoingMessageRegister(), "Outgoing message ledger");
		}
		System.out.println("Cache generated");
	}

	private void generateTransitMessageCache(ArrayList<? extends ProofOfDelivery> documents, String cacheName)
	{
		NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
				.action(cacheName)
				.data(new LogData()
						.entry("count", String.valueOf(documents.size()))
						.entry("port", String.valueOf(port))
						.toString())
				.description("Commencing Cache Building")
				.logInfo());
		documents.forEach(doc -> {
			doc.saveInCache();
		});
		NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
				.action(cacheName)
				.description(cacheName + " Building Completed")
				.logInfo());
		System.out.println(cacheName + " of size " + documents.size() + " build successfully in " + String.valueOf(System.currentTimeMillis()-start) + "ms");
	}

	private void fillMasterLedger(ArrayList<? extends ProofOfDelivery> documents, MasterMessageLedger ledger, String ledgerName)
	{
		NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
				.action(ledgerName)
				.data("count" + documents.size())
				.description("Commencing Ledger Building")
				.data(new LogData()
						.entry("count", String.valueOf(documents.size()))
						.entry("port", String.valueOf(port))
						.toString())
				.logInfo());
		
		documents.forEach(doc -> {
			ledger.add(doc.getProducerNodeId(), doc.getEventType(), doc.getMid());
		});
		
		NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
				.action(ledgerName)
				.description(ledgerName + " Building Completed")
				.logInfo());
		System.out.println(ledgerName + " of size " + documents.size() + " build successfully in " + String.valueOf(System.currentTimeMillis()-start) + "ms");
	}
}