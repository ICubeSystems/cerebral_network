package com.ics.nceph.core.message;

import com.ics.nceph.core.message.data.MessageData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.util.ByteUtil;


/**
 * @version 1.0
 * @author Anshul
 * @since 24-Mar-2022
 */
public class AcknowledgeMessage extends Message 
{
	public AcknowledgeMessage(byte type, byte eventType, byte[] data, byte[] messageId, byte[] sourceId)
	{
		super(eventType, type, data, messageId, sourceId);
	}
	
	/**
	 * 
	 * @author Anshul
	 */
	public static class Builder
	{
		private byte eventType;
		
		private byte type;
		
		private byte[] data;
		
		private byte[] messageId;
		
		private byte[] sourceId;
		
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
		
		public Builder mid(String mid) {
			String[] idArray = mid.split("-",2);
			this.messageId = ByteUtil.convertToByteArray(Integer.valueOf(idArray[1]), 6);
			this.sourceId = ByteUtil.convertToByteArray(Integer.valueOf(idArray[0]), 2);
			return this;
		}
		
		public AcknowledgeMessage build()
		{
			eventType = Integer.valueOf(0).byteValue(); 
			return new AcknowledgeMessage(type, eventType, data, messageId, sourceId);
		}
	}
}
