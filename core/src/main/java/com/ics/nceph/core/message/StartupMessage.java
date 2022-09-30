package com.ics.nceph.core.message;

import com.ics.id.exception.IdGenerationFailedException;
import com.ics.nceph.core.message.data.MessageData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 29-Mar-2022
 */
public class StartupMessage extends Message
{
	StartupMessage(byte type, byte eventType, byte[] data, byte[]originatingPort) throws IdGenerationFailedException 
	{
		super(eventType, type, data, originatingPort);
	}

	public static class Builder
	{
		private byte eventType;

		private byte type = 0;
		
		private byte[] originatingPort;

		private byte[] data;

		public Builder data(MessageData message) throws MessageBuildFailedException 
		{
			this.data = message.bytes();
			return this;
		}
		
		public Builder originatingPort(Integer originatingPort) {
			this.originatingPort = ByteUtil.convertToByteArray(originatingPort, 2);
			return this;
		}
		
		public StartupMessage build() throws IdGenerationFailedException 
		{
			eventType = Integer.valueOf(0).byteValue();
			return new StartupMessage(type, eventType, data, originatingPort);
		}
	}
}
