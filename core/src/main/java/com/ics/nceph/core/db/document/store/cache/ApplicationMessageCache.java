package com.ics.nceph.core.db.document.store.cache;

import java.util.concurrent.ConcurrentHashMap;

import com.ics.nceph.core.db.document.MessageDocument;

/**
 * This is a generic map of application port and {@link MessageDocument messages} 
 * @author Anshul
 * @version 1.0
 * @since Sep 27, 2022
 */
public class ApplicationMessageCache<T extends MessageDocument> extends ConcurrentHashMap<Integer, MessageCache<T>> 
{
	private static final long serialVersionUID = -3961453832872181115L;
	
	public void put(Integer key, T messageDocument) 
	{	
		MessageCache<T> messageCache = get(key);
		if(messageCache == null) 
			messageCache = new MessageCache<>();
		messageCache.put(messageDocument.getMessageId(), messageDocument);
		put(key, messageCache);
	}
	
	public T getDocument(Integer port, String docName) {
		return get(port).getDocument(docName);
	}
	
	public MessageCache<T> getMessageCache(Integer port) {
		return get(port);
	}
	
	public void removeFromCache(Integer key, T messageDocument) {
		get(key).remove(messageDocument.getMessageId());
	}
}
