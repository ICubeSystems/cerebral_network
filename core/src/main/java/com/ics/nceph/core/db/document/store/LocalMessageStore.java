package com.ics.nceph.core.db.document.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.db.document.MessageDocument;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public class LocalMessageStore extends DocumentStore
{	
	final ObjectMapper mapper = new ObjectMapper()
			.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.setSerializationInclusion(Include.NON_NULL);
	
	/**
	 * Create a new ProofOfPublish / ProofOfAuthentication document or save the updates in the local document store
	 * 
	 * @param pod
	 * @param docName
	 * @return void
	 */
	private void saveUpdate(MessageDocument document, String docName, boolean isUpdate) throws DocumentSaveFailedException
	{
		try 
		{
			try 
			{
				// If the document is not in cache, then add the new document to the cache. This should be the case when the document is being created for the very first time
				document.saveInCache();
				// Save the document to the local storage
				mapper.writeValue(Paths.get(document.localRepository() + docName + ".json").toFile(), document);
			} catch (FileNotFoundException fe) // In case the message directory is missing 
			{
				// Create new directory
				new File(Paths.get(document.localRepository()).toString()).mkdirs();
				// Save the document to the local storage
				mapper.writeValue(Paths.get(document.localRepository() + docName + ".json").toFile(), document);
			}
			super.save(document, docName, isUpdate);
		} catch (IOException e) {
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(docName)
					.action(isUpdate ? (document.getClass().getSimpleName() + " updation failed") : (document.getClass().getSimpleName() + " creation failed"))
					.description("Error while updating the " + document.getClass().getSimpleName())
					.logError(),e);
			throw new DocumentSaveFailedException(docName+".json file write failed", e);
		}
	}
	
	@Override
	public void save(MessageDocument document, String docName) throws DocumentSaveFailedException
	{
		saveUpdate(document, docName, false);
	}
	
	/**
	 * 
	 * @param document
	 * @param docName
	 * @throws DocumentSaveFailedException
	 */
	@Override
	public void update(MessageDocument document, String docName) throws DocumentSaveFailedException
	{
		File file = new File(document.localRepository() + docName + ".json");
		// Check if the file exists
		if(file.exists()) 
		{
			// Save the document
			saveUpdate(document, docName, true);
			return;
		}
		// Log warning if the file does not exists
		NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
				.messageId(docName)
				.action(document.getClass().getSimpleName() + " updation failed")
				.description(docName + ".json not found")
				.logError());
	}
	
	/**
	 * 
	 * @param docName
	 * @param document
	 * @return
	 */
	@Override
	public boolean delete(String docName, MessageDocument document)
	{
		File file = new File( document.localRepository() + docName + ".json");
		// If file is deleted successfully then remove from cache and return
		if(file.delete()) 
		{
			//			cache.remove(docName, document);
			//Log
			
			NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
					.messageId(docName)
					.action(document.getClass().getSimpleName() + " deleted")
					.logInfo());
			return true;
		}
		//Log if file is not deleted
		NcephLogger.MESSAGE_LOGGER.warn(new MessageLog.Builder()
				.messageId(docName)
				.action(document.getClass().getSimpleName() + " deletion failed")
				.logError());
		return false;
	}
}
