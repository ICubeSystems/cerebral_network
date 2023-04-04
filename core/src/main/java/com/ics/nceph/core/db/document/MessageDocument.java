package com.ics.nceph.core.db.document;

import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Base class of local messages 
 * 
 * @author Anshul
 * @since 11-Apr-2022
 */
 // Indicates that a class can be serialized as an Amazon DynamoDB document
@Getter
@Setter
public abstract class MessageDocument extends Document
{
	/**
     * Contract method to return location of the local document store
     */
	@JsonIgnore
	@DynamoDBIgnore
	public abstract String localRepository();
	
	/**
	 * Message Id {@link messageId} 
	 */
	private String messageId;
	
	/**
	 * Message sender Port Number {@link producerPortNumber} 
	 */
	private Integer producerPortNumber;
	
	/**
	 * Contract method used to insert new document in cache.
	 */
	public abstract void saveInCache();
	
	/**
	 * Contract method used to remove from cache
	 */
	public abstract void removeFromCache();
	
	/**
	 * Contract method used to save document in DB
	 * @throws DocumentSaveFailedException
	 */
	public abstract void saveInDB() throws DocumentSaveFailedException;
	
	private long createdOn;
	
	private long updatedOn;
	
	@JsonIgnore
	@DynamoDBIgnore
	@Setter(AccessLevel.NONE) //Mutator for this attribute will be omitted
	private boolean sync = true;
	
	@DynamoDBIgnore
	@JsonIgnore
	private List<String> changeLog;
	
	public MessageDocument() {
		super();
		changeLog = new ArrayList<String>();
	}
	
	public void setMessageId(String messageId) 
	{
		this.messageId = messageId;
		outOfSync("messageId");
	}
	
	public void setProducerPortNumber(Integer producerPortNumber) 
	{
		this.producerPortNumber = producerPortNumber;
		outOfSync("ProducerPortNumber");
	}
	
	/**
	 * 
	 * @param fieldName
	 */
	public void outOfSync(String fieldName) 
	{
		this.sync = false;
		changeLog.add(fieldName);
	}
	
	/**
	 * 
	 */
	public void inSync() 
	{
		this.sync = true;
		changeLog.clear();
	}
}
