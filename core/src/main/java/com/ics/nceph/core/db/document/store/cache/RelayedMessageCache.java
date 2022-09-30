package com.ics.nceph.core.db.document.store.cache;

import java.util.HashMap;

import com.ics.nceph.core.db.document.ProofOfRelay;

/**
 * This is a map of producer port and {@link ReceivedMessageCache}
 * @author Anshul
 * @version 1.0
 * @since Sep 06, 2022
 */
public class RelayedMessageCache extends HashMap<Integer, ApplicationMessageCache<ProofOfRelay>> 
{
	private static final long serialVersionUID = -1805738486763899058L;

	public void put(ProofOfRelay por) 
	{
		ApplicationMessageCache<ProofOfRelay> porCacheValue = get(por.getProducerPortNumber());
		if(porCacheValue == null) 
			porCacheValue = new ApplicationMessageCache<ProofOfRelay>();
		porCacheValue.put(por.getConsumerPortNumber(), por);
		put(por.getProducerPortNumber(), porCacheValue);
	}
	
	public ProofOfRelay getDocument(Integer producerPort,Integer consumerPort, String docName) {
		return get(producerPort).getDocument(consumerPort, docName);
	}
	
	public MessageCache<ProofOfRelay> getMessageCache(Integer producerPort, Integer consumerPort) {
		return get(producerPort).getMessageCache(consumerPort);
	}
	
	public void removeFromCache(ProofOfRelay por) {
		get(por.getProducerPortNumber()).removeFromCache(por.getConsumerPortNumber(), por);;
	}
}
