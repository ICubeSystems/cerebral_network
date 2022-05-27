package com.ics.nceph.core.message;

import java.util.BitSet;
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
	AuthenticationMessage(byte type, byte flags, byte[] data, byte[] messageId, byte[] sourceId) 
	{
		super(flags, type, data, messageId, sourceId);
	}

	public static class Builder
	{
		private BitSet flags = new BitSet(8);

		private byte type;

		private byte[] data;

		private byte[] messageId;

		private byte[] sourceId;

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

		public AuthenticationMessage build() 
		{
			flags.set(MessageFlag.TRACE_FLAG.getPosition());
			return new AuthenticationMessage(type, flags.toByteArray()[0], data, messageId, sourceId);
		}
	}
}
