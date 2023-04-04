package com.ics.nceph.core.db.document.store;

import java.util.List;

import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.db.document.MessageDocument;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;

/**
 * Singleton implementation of 
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public abstract class DocumentStore 
{	
	/**
	 * Singleton instance of DocumentStore
	 */
	private static DocumentStore documentStore;
	
	/**
	 * 
	 * @param document
	 * @param docName
	 * @throws DocumentSaveFailedException
	 */
	public abstract void save(MessageDocument document, String docName) throws DocumentSaveFailedException;
	
	/**
	 * 
	 * @param document
	 * @param docName
	 * @throws DocumentSaveFailedException
	 */
	public abstract void update(MessageDocument document, String docName) throws DocumentSaveFailedException;
	
	/**
	 * 
	 * @param docName
	 * @param document
	 * @return boolean
	 */
	public abstract boolean delete(String docName, MessageDocument document);
	
	/**
	 * Returns the instance of DocumentStore
	 * @since Sep 14, 2022
	 */
	public static DocumentStore getInstance()
	{
		if (documentStore == null)
		{
			if(NcephConstants.saveInDB) 
				documentStore = new DynamoDbMessageStore();
			else 
				documentStore = new LocalMessageStore();
		}
		return documentStore;
	}
	
	protected void save(MessageDocument document, String docName, boolean isUpdate) throws DocumentSaveFailedException
	{
		if(document.getChangeLog().size()>0) 
		{    
			List<String> changelog = document.getChangeLog();
			synchronized (changelog) 
			{
				try 
				{
					NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
							.messageId(docName)
							.action(isUpdate ? (document.getClass().getSimpleName() + " updated") : (document.getClass().getSimpleName() + " saved"))
							.description(String.join(", ", changelog))
							.data(
									new LogData()
									.entry("CallerClass", Thread.currentThread().getStackTrace()[isUpdate?3:2].getFileName())
									.toString())
							.logInfo());
				} catch (Exception e) {}
			}
		}
		else{
			NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
					.messageId(docName)
					.action(isUpdate ? (document.getClass().getSimpleName() + " updated") : (document.getClass().getSimpleName() + " saved"))
					.description("Blank Save")
					.data(
							new LogData()
							.entry("CallerClass", Thread.currentThread().getStackTrace()[isUpdate?3:2].getFileName())
							.toString())
					.logInfo());
		}
		document.inSync();
	}
}
