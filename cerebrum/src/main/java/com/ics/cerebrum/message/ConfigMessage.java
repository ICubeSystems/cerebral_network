package com.ics.cerebrum.message;

import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.MessageData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.util.ByteUtil;
/**
 * This message is sent by the cerebrum in response to the {@link BootstrapMessage}. This message contains the following data:<br>
 * <ol>
 * 	<li>NodeId - unique identifier of a synaptic node in the nceph network</li>
 * 	<li>Map of EventType and ApplicationReceptor</li>
 * </ol>
 * 
 * @author Anshul
 * @since 28-Jul-2022
 */
public class ConfigMessage extends Message
{
	ConfigMessage(byte type, byte eventType, byte[] data, byte[] messageId, byte[] sourceId,  byte[] originatingPort) {
		super(eventType, type, data, messageId, sourceId, originatingPort);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Builder class
	 * 
	 * @author Anshul
	 * @since 28-Jul-2022
	 */
	public static class Builder
	{
		private byte eventType;
		
		private byte type = 16;
		
		private byte[] data;
		
		private byte[] messageId;
		
		private byte[] sourceId;
		
		private byte[] originatingPort;
		
		public Builder data(MessageData messageData) throws MessageBuildFailedException
		{
			this.data = messageData.bytes();
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
		
		public Builder originatingPort(Integer originatingPort) {
			this.originatingPort = ByteUtil.convertToByteArray(originatingPort, 2);
			return this;
		}
		
		public Builder mid(String mid) {
			String[] idArray = mid.split("-",2);
			this.messageId = ByteUtil.convertToByteArray(Integer.valueOf(idArray[1]), 6);
			this.sourceId = ByteUtil.convertToByteArray(Integer.valueOf(idArray[0]), 2);
			return this;
		}
		
		public ConfigMessage build()
		{
			eventType = Integer.valueOf(0).byteValue(); 
			return new ConfigMessage(type, eventType, data, messageId, sourceId, originatingPort);
		}
	}
}
