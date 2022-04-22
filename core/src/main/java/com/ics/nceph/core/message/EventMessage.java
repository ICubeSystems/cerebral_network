package com.ics.nceph.core.message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.event.Event;

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
	
	/**
	 * 
	 * @author Anurag Arya
	 */
	public static class Builder
	{
		private BitSet flags = new BitSet(8);
		
		private byte type = 0x03;
		
		private byte[] data;
		
		public Builder event(Event event) throws IOException 
		{
			ObjectMapper mapper = new ObjectMapper();
			String eventJSON = mapper.writeValueAsString(event);
			this.data = eventJSON.getBytes(StandardCharsets.UTF_8);
			return this;
		}
		
		public EventMessage build() 
		{
			flags.set(MessageFlag.TRACE_FLAG.getPosition());
			return new EventMessage(type, flags.toByteArray()[0], data);
		}
	}
}
