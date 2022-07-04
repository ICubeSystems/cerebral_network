package com.ics.nceph.core.message;

import com.ics.id.exception.IdGenerationFailedException;
import com.ics.nceph.core.message.data.MessageData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 29-Mar-2022
 */
public class StartupMessage extends Message
{
	StartupMessage(byte type, byte eventType, byte[] data) throws IdGenerationFailedException 
	{
		super(eventType, type, data);
	}

	public static class Builder
	{
		private byte eventType;

		private byte type = 0x00;

		private byte[] data;

		public Builder data(MessageData message) throws MessageBuildFailedException 
		{
			this.data = message.bytes();
			return this;
		}

		public StartupMessage build() throws IdGenerationFailedException 
		{
			eventType = Integer.valueOf(0).byteValue();
			return new StartupMessage(type, eventType, data);
		}
	}
}
