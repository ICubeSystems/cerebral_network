package com.ics.nceph.core.event;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import com.ics.nceph.core.message.data.MessageData;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 24-Dec-2021
 */
public class EventData extends MessageData implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//TODO this should be renamed to eventType. Then introduce eventId as well
	Integer eventId;

	String objectJSON;
	
	long createdOn;
	
	public EventData() {}
	
	private EventData(Integer eventId, String objectJSON)
	{
		createdOn = new Date().getTime();
		this.eventId = eventId;
		this.objectJSON = objectJSON;
	}

	public String getObjectJSON() {
		return objectJSON;
	}

	public Integer getEventId() {
		return eventId;
	}
	
	public long getCreatedOn() {
		return createdOn;
	}
	
	/**
	 * 
	 * 
	 * @throws IOException
	 * @return byte[]
	 */
	public byte[] toBytes() throws IOException
	{
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
		objectOutputStream.writeObject(this);
		objectOutputStream.flush();
		byte[] bytes = byteArrayOutputStream.toByteArray();
		objectOutputStream.close();
		byteArrayOutputStream.close();
		return bytes;
	}
	
	public static class Builder
	{
		private Integer eventId;

		private String objectJSON;
		
		public Builder eventId(Integer eventId)
		{
			this.eventId = eventId;
			return this;
		}
		
		public Builder objectJSON(String objectJSON)
		{
			this.objectJSON = objectJSON;
			return this;
		}
		
		public EventData build()
		{
			return new EventData(eventId, objectJSON);
		}
	}
}
