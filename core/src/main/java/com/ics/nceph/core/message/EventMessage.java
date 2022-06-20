package com.ics.nceph.core.message;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	EventMessage(byte type, byte flags, byte[] data)
	{
		super(flags, type, data);
	}
	
	EventMessage(byte type, byte flags, byte[] data, byte[] messageId, byte[] sourceId)
	{
		super(flags, type, data, messageId, sourceId);
	}
	/**
	 * 
	 * @author Anurag Arya
	 */
	public static class Builder
	{
		private BitSet flags = new BitSet(8);
		
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
		
		public EventMessage build() 
		{
			flags.set(MessageFlag.TRACE_FLAG.getPosition());
			return new EventMessage(type, flags.toByteArray()[0], data);
		}
		
		/**
		 * 
		 * @return
		 */
		public EventMessage buildAgain() 
		{
			flags.set(MessageFlag.TRACE_FLAG.getPosition());
			return new EventMessage(type, flags.toByteArray()[0], data, messageId, sourceId);
		}
	}
}
