package com.ics.cerebrum.db.store.cache;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.ics.logger.BootstraperLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.db.document.MessageDocument;
import com.ics.nceph.core.db.document.ProofOfDelivery;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.exception.CacheInitializationException;
import com.ics.nceph.core.message.MasterMessageLedger;

/**
 * This class is used to initialize {@link DocumentCache} and {@link MasterMessageLedger} from local database
 * @author Anshul
 * @version 1.0
 * @since Sep 28, 2022
 */
public class LocalStoreCacheInitializer extends CerebrumCacheInitializer
{
	final ObjectMapper mapper = new ObjectMapper()
			.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.setSerializationInclusion(Include.NON_NULL);

	private long start;
	
	LocalStoreCacheInitializer() throws CacheInitializationException
	{
		initialize();
	}
	
	void initialize() throws CacheInitializationException 
	{
		if (CerebrumCacheInitializer.intializer != null)
			throw new CacheInitializationException("Cache already intialized");
		for (Map.Entry<Integer, Connector> entry : ConnectorCluster.activeConnectors.entrySet())
		{
			//LOG
			NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
					.action("Building cache")
					.description("Start filling published cache and incoming message register")
					.logInfo());
			start = System.currentTimeMillis();
			generateCacheAndMessageLedger(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location")+entry.getKey()+"/",ProofOfPublish.class, entry.getValue().getIncomingMessageRegister(), "Published cache and incoming message register", 600);
			//LOG
			NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
					.action("Success")
					.description("published cache and incoming message register filled successfully")
					.logInfo());
			// LOG
			NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
					.action("Building cache")
					.description("Start filling relayed cache and outgoing message register")
					.logInfo());
			start = System.currentTimeMillis();
			generateCacheAndMessageLedger(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.relayed_location")+entry.getKey()+"/",ProofOfRelay.class, entry.getValue().getOutgoingMessageRegister(), "Received cache and outgoing message register", 500);
			//LOG
			NcephLogger.BOOTSTRAP_LOGGER.info(new BootstraperLog.Builder()
					.action("Success")
					.description("relayed cache and outgoing message register filled successfully")
					.logInfo());
		}
	}
	
	private void generateCacheAndMessageLedger(String localPath, Class<? extends MessageDocument> document, MasterMessageLedger ledger, String cacheName, int cacheCondition) throws CacheInitializationException
	{
		try
		{
			File[] messageDirectory = new File(localPath).listFiles();
			if(messageDirectory != null) 
			{
				for (File file : messageDirectory) {
					if (file.isDirectory()) { 
						generateCacheAndMessageLedger(localPath+file.getName(), document, ledger, cacheName, cacheCondition); // Calls same method again.
					}
					else 
					{
						ProofOfDelivery doc;
						doc = (ProofOfDelivery)mapper.readValue(file, document);
						if(doc.getMessageDeliveryState() < cacheCondition)
							doc.saveInCache();
						ledger.add(doc.getProducerNodeId(), doc.getEventType(), doc.getMid());
					}
				}
			}
		} catch (IOException e)
		{
			throw new CacheInitializationException("IO exception in build cache from local store", e);
		}
		System.out.println(cacheName + " build successfully in " + String.valueOf(System.currentTimeMillis()-start) + "ms");
	}
}
