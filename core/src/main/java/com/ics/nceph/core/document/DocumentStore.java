package com.ics.nceph.core.document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public class DocumentStore 
{
	private static ConcurrentHashMap<String, Document> cache;
	
	private static final ObjectMapper mapper = new ObjectMapper()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
			.setSerializationInclusion(Include.NON_NULL);

	
	public static void initiate()
	{
		if (cache == null)
			cache = new ConcurrentHashMap<String, Document>();
		
		// Monitor thread - 5 Mins
	}
	
	
	/**
	 * Create a new ProofOfDelivery document or save the updates in the local document store
	 * 
	 * @param pod
	 * @param docName
	 * @return void
	 */

	public synchronized static void save(Document document, String docName) throws IOException
	{
		try 
		{
			try 
			{
				// If the POD is not in cache, then add the new POD to the cache. This should be the case when the POD is being created for the very first time
				if (!cache.containsKey(docName))
					cache.put(docName, document);
				
				// Save the document to the local storage
				mapper.writeValue(Paths.get(document.localMessageStoreLocation() + docName + ".json").toFile(), document);
			} catch (FileNotFoundException fe) // In case the message directory is missing 
			{
				// Create new directory
				new File(Paths.get(document.localMessageStoreLocation()).toString()).mkdirs();
				// Save the document to the local storage
				mapper.writeValue(Paths.get(document.localMessageStoreLocation() + docName + ".json").toFile(), document);
			}
			NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
					.messageId(docName)
					.action("POD saved")
					.description(String.join(", ", document.changeLog))
					.logInfo());
			document.inSync();
			
		} catch (IOException e) {

			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(docName)
					.action("POD write failed")
					.description("Error while updating the POD")
					.logError(),e);
			throw e;
		}
		
	}

	/**
	 * Loads the ProofOfDelivery from document cache
	 * 
	 * @param docName
	 * @return Document
	 */
	public synchronized static Document load(String docName)
	{
		return cache.get(docName);
	}

	public static ProofOfDelivery load(File pod)
	{
		try 
		{
			return mapper.readValue(pod, ProofOfDelivery.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static boolean delete(String docName, Document document) 
	{
		File file = new File( document.localMessageStoreLocation() + docName + ".json");
		if(file.delete()) 
		{
			System.out.println(docName + " deleted successfully");
			//Log
			NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
					.messageId(docName)
					.action("deleted")
					.description("POD deleted successfully")
					.logInfo());
			return true;
		}
		//Log
		NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
				.messageId(docName)
				.action("Error in delete")
				.logError());
		return false;
	}
}
