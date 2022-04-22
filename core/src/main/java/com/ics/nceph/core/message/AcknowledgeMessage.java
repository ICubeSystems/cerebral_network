package com.ics.nceph.core.message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @version 1.0
 * @author Anshul
 * @since 24-Mar-2022
 */
public class AcknowledgeMessage extends Message 
{
	AcknowledgeMessage(byte type, byte flags, byte[] data, byte[] messageId, byte[] sourceId)
	{
		super(flags, type, data, messageId, sourceId);
	}
	
	/**
	 * 
	 * @author Anshul
	 */
	public static class Builder
	{
		private BitSet flags = new BitSet(8);
		
		private byte type;
		
		private byte[] data;
		
		private byte[] messageId;
		
		private byte[] sourceId;
		
		public Builder data(MessageData messageData) throws IOException 
		{
			ObjectMapper mapper = new ObjectMapper();
			String eventJSON = mapper.writeValueAsString(messageData);
			this.data = eventJSON.getBytes(StandardCharsets.UTF_8);
			//this.data = event.toBytes();
			//System.out.println("Event Object Length: " + data.length);
			return this;
		}
		
		public Builder messageId(byte[] messageId) {
			this.messageId = messageId;
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
		
		public AcknowledgeMessage build() 
		{
			flags.set(MessageFlag.TRACE_FLAG.getPosition()); // BAD CODE - remove later
			return new AcknowledgeMessage(type, flags.toByteArray()[0], data, messageId, sourceId);
		}
	}
}
