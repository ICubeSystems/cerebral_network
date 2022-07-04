package com.ics.nceph.core.message;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.id.exception.IdGenerationFailedException;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 31-Dec-2021
 */
public class EventMessage extends Message 
{
	EventMessage(byte type, byte eventType, byte[] data) throws IdGenerationFailedException
	{
		super(eventType, type, data);
	}
	
	EventMessage(byte type, byte eventType, byte[] data, byte[] messageId, byte[] sourceId)
	{
		super(eventType, type, data, messageId, sourceId);
	}
	/**
	 * 
	 * @author Anurag Arya
	 */
	public static class Builder
	{
		private byte eventType;
		
		private byte type = 0x03;
		
		private byte[] data;
		
		private byte[] messageId;
		
		private byte[] sourceId;
		
		public Builder event(EventData event) throws MessageBuildFailedException 
		{
			try {
				ObjectMapper mapper = new ObjectMapper();
				String eventJSON = mapper.writeValueAsString(event);
				this.data = eventJSON.getBytes(StandardCharsets.UTF_8);
				this.eventType = Integer.valueOf(event.getEventType()).byteValue();
			} catch (JsonProcessingException e) {
				throw new MessageBuildFailedException("JsonProcessingException", e);
			}
			return this;
		}
		
		public Builder messageId(byte[] messageId) {
			this.messageId = messageId;
			return this;
		}
		
		public Builder mid(String mid) {
			String[] idArray = mid.split("-",2);
			this.messageId = ByteUtil.convertToByteArray(Integer.valueOf(idArray[1]), 6);
			this.sourceId = ByteUtil.convertToByteArray(Integer.valueOf(idArray[0]), 2);
			return this;
		}
		
		public Builder type(byte type) {
			this.type = type;
			return this;
		}
		
		public Builder sourceId(byte[] sourceId) {
			this.sourceId = sourceId;
			return this;
		}
		
		public EventMessage build() throws IdGenerationFailedException 
		{
			return new EventMessage(type, eventType, data);
		}
		
		/**
		 * 
		 * @return
		 */
		public EventMessage buildAgain() 
		{
			return new EventMessage(type, eventType, data, messageId, sourceId);
		}
	}
}
