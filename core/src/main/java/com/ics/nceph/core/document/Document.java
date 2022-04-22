package com.ics.nceph.core.document;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class of local messages 
 * @author Anshul
 * @since 11-Apr-2022
 */
public abstract class Document 
{
	@JsonIgnore
	private boolean sync = true;
	
	@JsonIgnore
	ArrayList<String> changeLog;
	
	public abstract String localMessageStoreLocation();

	public void outOfSync(String fieldName) 
	{
		this.sync = false;
		changeLog.add(fieldName);
	}
	
	public void inSync() 
	{
		this.sync = true;
		changeLog.clear();
	}

	public boolean isSync() {
		return sync;
	}
}
