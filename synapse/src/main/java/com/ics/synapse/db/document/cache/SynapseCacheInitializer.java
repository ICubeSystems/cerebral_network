package com.ics.synapse.db.document.cache;

import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.db.document.exception.CacheInitializationException;

/**
 * Contract class for {@link DocumentCache cache} initialization
 * 
 * @author Anshul
 * @version 1.0
 * @since Sep 27, 2022
 */
public abstract class SynapseCacheInitializer
{
	static SynapseCacheInitializer intializer;
	
	/**
	 * Run the appropriate CacheInitializer instance
	 * @throws CacheInitializationException
	 */
	public static void run() throws CacheInitializationException 
	{
		if (intializer == null)
		{
			try
			{
				if(NcephConstants.saveInDB)
					intializer = new DynamoDBCacheInitializer();
				else 
					intializer = new LocalStoreCacheInitializer();
			} catch (CacheInitializationException e)
			{
				// LOG
				throw e;
			}
		}
	}
	
	/**
	 * Contract method to initialize the cache
	 * @throws CacheInitializationException
	 */
	abstract void initialize() throws CacheInitializationException ;
}
