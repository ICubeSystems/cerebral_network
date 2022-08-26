package com.ics.nceph.core.document;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class of local messages 
 * 
 * @author Anshul
 * @since 11-Apr-2022
 */
public abstract class Document 
{
	private String messageId;
	
	public long createdOn;
	
	@JsonIgnore
	private boolean sync = true;
	
	@JsonIgnore
	ArrayList<String> changeLog;
	
	public abstract String localMessageStoreLocation();
	
	@JsonIgnore
	public abstract String getName();
	
	public Document() {}
	
	public Document(String messageId) 
	{
		this.messageId = messageId;
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(String messageId) 
	{
		this.messageId = messageId;
		outOfSync("messageId");
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

	/**
	 * 
	 * @return
	 */
	public boolean isSync() {
		return sync;
	}
}
