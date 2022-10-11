package com.ics.synapse.db.document.cache;

import java.util.ArrayList;
import java.util.Map;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.db.document.ProofOfDelivery;
import com.ics.nceph.core.db.document.exception.CacheInitializationException;
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
class DynamoDBCacheInitializer extends SynapseCacheInitializer
{	
	DynamoDBCacheInitializer() throws CacheInitializationException
	{
		initialize();
	}
	
	void initialize() throws CacheInitializationException 
	{
		if (SynapseCacheInitializer.intializer != null)
			throw new CacheInitializationException("Cache already intialized");
		for (Map.Entry<Integer, Connector> entry : ConnectorCluster.activeConnectors.entrySet())
		{
			generateTransitMessageCache(ApplicationContextUtils.context.getBean("publishedMessageRepository", PublishedMessageRepository.class).findAllByPartitionKeyAndMessageDeliveryStateLessThan("P:"+entry.getKey(), 500));
			generateTransitMessageCache(ApplicationContextUtils.context.getBean("receivedMessageRepository", ReceivedMessageRepository.class).findAllByPartitionKeyStartingWithAndProducerPortNumberAndMessageDeliveryStateLessThan("R:",entry.getKey(), 500));
			fillMasterLedger(ApplicationContextUtils.context.getBean("publishedMessageRepository", PublishedMessageRepository.class).findAllByPartitionKey("P:"+entry.getKey()), entry.getValue().getOutgoingMessageRegister());
			fillMasterLedger(ApplicationContextUtils.context.getBean("receivedMessageRepository", ReceivedMessageRepository.class).findAllByPartitionKey("R:"+entry.getKey()), entry.getValue().getIncomingMessageRegister());
		}
	}
	
	private void generateTransitMessageCache(ArrayList<? extends ProofOfDelivery> documents)
	{
		documents.forEach(doc -> {
			doc.saveInCache();
		});
	}

	private void fillMasterLedger(ArrayList<? extends ProofOfDelivery> documents, MasterMessageLedger ledger)
	{
		documents.forEach(doc -> {
			ledger.add(doc.getProducerNodeId(), doc.getEventType(), doc.getMid());
		});
	}
}