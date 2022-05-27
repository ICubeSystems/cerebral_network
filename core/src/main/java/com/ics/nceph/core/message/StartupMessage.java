package com.ics.nceph.core.message;

import java.util.BitSet;
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
	StartupMessage(byte type, byte flags, byte[] data) 
	{
		super(flags, type, data);
	}

	public static class Builder
	{
		private BitSet flags = new BitSet(8);

		private byte type = 0x00;

		private byte[] data;

		public Builder data(MessageData message) throws MessageBuildFailedException 
		{
			this.data = message.bytes();
			return this;
		}

		public StartupMessage build() 
		{
			flags.set(MessageFlag.TRACE_FLAG.getPosition());
			return new StartupMessage(type, flags.toByteArray()[0], data);
		}
	}
}
