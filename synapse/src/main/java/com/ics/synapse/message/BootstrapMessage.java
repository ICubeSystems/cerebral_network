package com.ics.synapse.message;

import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.ReservedMessageId;
import com.ics.nceph.core.message.data.MessageData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.util.ByteUtil;

/**
 * This message is sent by the synapse at the time of bootstrapping. The message contains MAC address of the synaptic node for name resolution on the cerebrum.
 * 
 * @author Anshul
 * @version 1.0
 * @since 28-Jul-2022
 */
public class BootstrapMessage extends Message
{
	BootstrapMessage(byte type, byte eventType, byte[] data, byte[] messageId, byte[] sourceId, byte[]originatingPort) 
	{
		super(eventType, type, data, messageId, sourceId, originatingPort);
	}

	public static class Builder
	{
		private byte eventType;
		
		private byte type = 15;
		
		private byte[] data;
		
		private byte[] messageId;
		
		private byte[] sourceId;
		
		private byte[] originatingPort;
		
		public Builder data(MessageData messageData) throws MessageBuildFailedException
		{
			this.data = messageData.bytes();
			return this;
		}
		
		public Builder messageId(byte[] messageId) 
		{
			this.messageId = messageId;
			return this;
		}
		
		public Builder type(byte type) 
		{
			this.type = type;
			return this;
		}
		
		public Builder sourceId(byte[] sourceId) 
		{
			this.sourceId = sourceId;
			return this;
		} 
		
		public Builder originatingPort(Integer originatingPort) {
			this.originatingPort = ByteUtil.convertToByteArray(originatingPort, 2);
			return this;
		}
		
		public BootstrapMessage build()
		{
			sourceId = ByteUtil.convertToByteArray(0, 2);
			messageId = ByteUtil.convertToByteArray(ReservedMessageId.CONTROL_CONNECTION_ID, 6); // bootstrap message will have a fixed message id per synaptic connector
			eventType = Integer.valueOf(0).byteValue(); 
			return new BootstrapMessage(type, eventType, data, messageId, sourceId, originatingPort);
		}
	}
}
