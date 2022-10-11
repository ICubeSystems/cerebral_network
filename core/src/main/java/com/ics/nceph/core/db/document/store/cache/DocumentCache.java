package com.ics.nceph.core.db.document.store.cache;

import java.io.IOException;

import com.ics.nceph.core.db.document.ProofOfAuthentication;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.exception.CacheInitializationException;

import lombok.Getter;

/**
 * Singleton container class for all the document caches required by cerebrum & synaptic nodes to function efficiently
 * 
 * @author Anshul
 * @version 1.0
 * @since Sep 06, 2022
 */
@Getter
public class DocumentCache
{
	/**
	 * This is a message cache which is used to store published messages
	 */
	private ApplicationMessageCache<ProofOfPublish> publishedMessageCache;
	
	/**
	 * This is a message cache which is used to store relayed messages
	 */
	private RelayedMessageCache relayedMessageCache;
	
	/**
	 * This is a message cache which is used to store authenticated messages
	 */
	private ApplicationMessageCache<ProofOfAuthentication> AuthenticationMessageCache;
	
	/**
	 * This is a singleton object of {@link DocumentCache}
	 */
	private static DocumentCache cache;
	
	/**
	 * Constructor of {@link DocumentCache}
	 * @throws CacheInitializationException 
	 * @throws IOException 
	 */
	private DocumentCache() throws CacheInitializationException 
	{
		publishedMessageCache = new ApplicationMessageCache<ProofOfPublish>();
		relayedMessageCache = new RelayedMessageCache();
		AuthenticationMessageCache = new ApplicationMessageCache<ProofOfAuthentication>();
	}
	
	public static void initialize() throws CacheInitializationException
	{
		if (cache == null)
			cache = new DocumentCache();
	}
	
	/**
	 * If Document cache is null, it creates an instance of {@link DocumentCache}. 
	 * And then return singleton instance of {@link DocumentCache}
	 * @return DocumentCache
	 * @throws CacheInitializationException 
	 * @throws IOException 
	 * @since Sep 23, 2022
	 */
	public static DocumentCache getInstance()
	{
		return cache;
	}
}
