package com.ics.nceph.core.document;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.document.exception.DocumentSaveFailedException;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public class DocumentStore 
{
	private static ConcurrentHashMap<String, Document> cache;

	private static boolean isUpdate;

	private static final ObjectMapper mapper = new ObjectMapper()
			.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
			.setSerializationInclusion(Include.NON_NULL);

	/**
	 * This method is used to initialize and build the cache on the node. This method is called from the bootstrapping classes.
	 * 
	 * @throws StreamReadException
	 * @throws DatabindException
	 * @throws IOException
	 */
	public static void initiate() throws IOException
	{
		// 1. If cache is null then create new cache
		if (cache == null)
			cache = new ConcurrentHashMap<String, Document>();

		// 2. Read the message directory on the local storage
		fillCache(new File(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location")),ProofOfDelivery.class);
		fillCache(new File(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.relayed_location")),ProofOfRelay.class);
	}

	private static void fillCache(File messageDirectory, Class<? extends Document> document) throws IOException {
		//If there are PODs in the local storage, then load them to cache
		if(messageDirectory.listFiles() != null) 
		{
			float totalPods = messageDirectory.listFiles().length;
			float loopCounter = 0;
			for (File podFile : messageDirectory.listFiles()) 
			{
				Document doc;
					doc = mapper.readValue(podFile, document);
					cache.put(podFile.getName().substring(0, podFile.getName().lastIndexOf(".")), doc);
				loopCounter++;
				float percentage = (loopCounter/totalPods)*100;
				// Print progress on console
				System.out.print((int)percentage+"%");
				if(percentage<10)
					System.out.print("\b\b");
				else if(percentage<100)
					System.out.print("\b\b\b");
				else
					System.out.println("\n");
			}
		}
	
	}

	/**
	 * 
	 * @return
	 */
	public static ConcurrentHashMap<String, Document> getCache() {
		return cache;
	}


	/**
	 * Create a new ProofOfDelivery / ProofOfAuthentication document or save the updates in the local document store
	 * 
	 * @param pod
	 * @param docName
	 * @return void
	 */
	public synchronized static void save(Document document, String docName) throws DocumentSaveFailedException
	{
		try 
		{
			try 
			{
				// If the document is not in cache, then add the new document to the cache. This should be the case when the document is being created for the very first time
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

			if(document.changeLog.size()>0) 
			{    
				ArrayList<String> changelog = document.changeLog;
				synchronized (changelog) 
				{
					try 
					{
						NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
								.messageId(docName)
								.action(isUpdate ? (document.getName() + " updated") : (document.getName() + " saved"))
								.description(String.join(", ", changelog))
								.data(
										new LogData()
										.entry("CallerClass", Thread.currentThread().getStackTrace()[isUpdate?3:2].getFileName())
										.toString())
								.logInfo());
					} catch (Exception e) {}
				}
			}
			document.inSync();
		} catch (IOException e) {
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(docName)
					.action(isUpdate ? (document.getName() + " updation failed") : (document.getName() + " creation failed"))
					.description("Error while updating the " + document.getName())
					.logError(),e);
			isUpdate = false;
			throw new DocumentSaveFailedException(docName+".json file write failed", e);

		}
		isUpdate = false;
	}

	/**
	 * 
	 * @param document
	 * @param docName
	 * @throws DocumentSaveFailedException
	 */
	public synchronized static void update(Document document, String docName) throws DocumentSaveFailedException 
	{
		isUpdate = true;
		File file = new File(document.localMessageStoreLocation() + docName + ".json");
		// Check if the file exists
		if(file.exists()) 
		{
			// Save the document
			save(document, docName);
			return;
		}
		// Log warning if the file does not exists
		NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
				.messageId(docName)
				.action(document.getName() + " updation failed")
				.description(docName + ".json not found")
				.logError());
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

	/**
	 * 
	 * @param document
	 * @return
	 */
	public static Document load(File document, Class<? extends Document> className)
	{
		try 
		{
			return mapper.readValue(document, className);
		} catch (IOException e) {
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(document.getName())
					.action("Mapping Error")
					.description(e.getMessage())
					.logError(),e);
		}
		return null;
	}
	public static void removeFromCache(String docName, Document document) {
		cache.remove(docName, document);
	}
	/**
	 * 
	 * @param docName
	 * @param document
	 * @return
	 */
	public static boolean delete(String docName, Document document) 
	{
		File file = new File( document.localMessageStoreLocation() + docName + ".json");
		// If file is deleted successfully then remove from cache and return
		if(file.delete()) 
		{
			cache.remove(docName, document);
			//Log
			NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
					.messageId(docName)
					.action(document.getName() + " deleted")
					.logInfo());
			return true;
		}
		//Log if file is not deleted
		NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
				.messageId(docName)
				.action(document.getName() + " deletion failed")
				.logError());
		return false;
	}
}
