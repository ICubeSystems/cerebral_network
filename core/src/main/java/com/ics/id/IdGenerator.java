package com.ics.id;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.ics.id.exception.IdGenerationFailedException;
import com.ics.logger.BootstraperLog;
import com.ics.logger.NcephLogger;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public class IdGenerator 
{
	private static IdCounter idCache;

	private static final ObjectMapper mapper = new ObjectMapper()
			.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
			.setSerializationInclusion(Include.NON_NULL);

	/**
	 * This method is used to initialize and build the idCache on the node. This method is called from the bootstrapping classes.
	 * 
	 */
	public static void initiate() 
	{
		idCache = new IdCounter();
		File file = new File(idCache.localIdCounterLocation()+idCache.IdCounterFileName());
		if(file.exists()) 
		{
			try 
			{
				idCache = mapper.readValue(Paths.get(idCache.localIdCounterLocation()+idCache.IdCounterFileName()).toFile(),IdCounter.class);
				return;
			} catch (IOException e) 
			{
				NcephLogger.BOOTSTRAP_LOGGER.error(new BootstraperLog.Builder()
						.action("Error building idCache")
						.description(e.getLocalizedMessage())
						.logError(),e);
			}
		}
	}

	/**
	 * 
	 * @return IdCounter
	 * @throws IdGenerationFailedException 
	 */
	public static Integer getId(Integer objectType) throws IdGenerationFailedException
	{
		// 1. Initialize id to 1
		Integer id = 1;
		try
		{
			// 2. If the sequence counter exists then increment and set the id
			id = idCache.getSequenceCounters().get(objectType).incrementAndGet();
		}catch (NullPointerException e)
		{
			// 3. If the sequence counter does not exists, then create new sequence counter for this objectType
			idCache.getSequenceCounters().put(objectType, new AtomicInteger(1));
		}
		// 4. Sync the id cache with the file
		save();
		// 5. Return the new id
		return id;
	}
	/**
	 * Create a new idCounter.json or save the updates in the local store
	 * 
	 * @param pod
	 * @param docName
	 * @return void
	 */
	public synchronized static void save() throws IdGenerationFailedException
	{
		try 
		{
			// Save the idCounter to the local storage
			mapper.writeValue(Paths.get(idCache.localIdCounterLocation()+idCache.IdCounterFileName()).toFile(), idCache);
		} 
		catch (IOException e) // In case the message directory is missing 
		{
			// If idCounter.json/ directory is not found at the expected location, then create the missing directory & file.
			if (e instanceof FileNotFoundException)
			{
				// Create new directory
				new File(Paths.get(idCache.localIdCounterLocation()).toString()).mkdirs();
				// Create & save idCounter.json
				try 
				{
					mapper.writeValue(Paths.get(idCache.localIdCounterLocation()+idCache.IdCounterFileName()).toFile(), idCache);
					return;
				} catch (IOException e1){}
			}
			// Log and throw exception in case exception is not FileNotFoundException 
			NcephLogger.GENERAL_LOGGER.error(new BootstraperLog.Builder()
					.action("Id sync failed")
					.description(e.getLocalizedMessage())
					.logError(),e);
			throw new IdGenerationFailedException("Sync of Id counters to persistant storage failed", e);
		}
	}
}
