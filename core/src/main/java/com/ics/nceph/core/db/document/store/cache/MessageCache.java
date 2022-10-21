package com.ics.nceph.core.db.document.store.cache;

import java.util.concurrent.ConcurrentHashMap;

import com.ics.nceph.core.db.document.MessageDocument;
/**
 * This is a map of mid and {@link MessageDocument messages} 
 * @author Anshul
 * @version 1.0
 * @since Sep 06, 2022
 */
public class MessageCache<V extends MessageDocument> extends ConcurrentHashMap<String, V>
{
	private static final long serialVersionUID = 4414351612167112964L;
	
	public V getDocument(String docName)
	{
		return get(docName);
	}
}
