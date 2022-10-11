package com.ics.nceph.core.message;

import com.ics.nceph.core.message.data.MessageData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 30-Mar-2022
 */
public class AuthenticationMessage extends Message
{
	AuthenticationMessage(byte type, byte eventType, byte[] data, byte[] messageId, byte[] sourceId, byte[]originatingPort) 
	{
		super(eventType, type, data, messageId, sourceId, originatingPort);
	}

	public static class Builder
	{
		private byte eventType;

		private byte type;

		private byte[] data;

		private byte[] messageId;

		private byte[] sourceId;
		
		private byte[] originatingPort;

		public Builder messageId(byte[] messageId)
		{
			this.messageId = messageId;
			return this;
		}

		public Builder sourceId(byte[] sourceId)
		{
			this.sourceId = sourceId;
			return this;
		}

		public Builder type(byte type) {
			this.type = type;
			return this;
		}
		
		public Builder data(MessageData messageData) throws MessageBuildFailedException
		{
			this.data = messageData.bytes();
			return this;
		}
		
		public Builder originatingPort(byte[] originatingPort) {
			this.originatingPort = originatingPort;
			return this;
		}
		
		public AuthenticationMessage build() 
		{
			eventType = Integer.valueOf(0).byteValue();
			return new AuthenticationMessage(type, eventType, data, messageId, sourceId, originatingPort);
		}
	}
}
