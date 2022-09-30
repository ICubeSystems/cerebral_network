package com.ics.nceph.core.db.document.store;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.db.document.MessageDocument;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Aug 19, 2022
 */
public class DynamoDbMessageStore extends DocumentStore
{
	/**
	 * Create a new ProofOfPublish / ProofOfAuthentication document or save the updates in the local document store
	 * 
	 * @param pod
	 * @param docName
	 * @return void
	 */
	private void saveUpdate(MessageDocument document, String docName, boolean isUpdate) throws DocumentSaveFailedException
	{
		// If the document is not in cache, then add the new document to the cache. This should be the case when the document is being created for the very first time
		document.saveInCache();
		try
		{
			// Save in DynamoDB
			document.saveInDB();
			// Post save work 
			super.save(document, docName, isUpdate);
		}catch (DocumentSaveFailedException e)
		{
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(docName)
					.action(isUpdate ? (document.getClass().getSimpleName() + " updation failed") : (document.getClass().getSimpleName() + " creation failed"))
					.description("Error while updating the " + document.getClass().getSimpleName())
					.logError(),e);
			throw e;
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
		// Save the document
		saveUpdate(document, docName, true);
	}

	@Override
	public boolean delete(String docName, MessageDocument document)
	{
		return false;
	}
}
