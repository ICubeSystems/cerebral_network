package com.ics.nceph.core.event;

import java.io.Serializable;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.ics.nceph.core.message.data.MessageData;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 24-Dec-2021
 */
@Getter
@Setter
@DynamoDBDocument
public class EventData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;
	
	Integer eventId;
	
	Integer eventType;

	String objectJSON;
	
	long createdOn;
	
	public EventData() {}
	
	public EventData(Integer eventId, Integer eventType, String objectJSON, long createdOn)
	{
		this.createdOn = createdOn;
		this.eventId = eventId;
		this.eventType = eventType;
		this.objectJSON = objectJSON;
	}

	public static class Builder
	{
		private Integer eventId;
		
		private Integer eventType;

		private String objectJSON;
		
		private long createdOn;
		
		public Builder eventId(Integer eventId)
		{
			this.eventId = eventId;
			return this;
		}
		
		public Builder eventType(Integer eventType)
		{
			this.eventType = eventType;
			return this;
		}
		
		public Builder objectJSON(String objectJSON)
		{
			this.objectJSON = objectJSON;
			return this;
		}
		
		public Builder createdOn(long createdOn)
		{
			this.createdOn = createdOn;
			return this;
		}
		
		public EventData build()
		{
			return new EventData(eventId, eventType, objectJSON, createdOn);
		}
	}
}
