package com.ics.nceph.core.message;

import com.ics.nceph.core.message.data.MessageData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.util.ByteUtil;


/**
 * This class is use to create message of type RESUME_TRANSMISSION. The messageId of this type of message is fixed to "1-2".
 * @author Anshul
 * @since 22-Nov-2022
 */
public class ResumeTransmissionMessage extends Message 
{
	public ResumeTransmissionMessage(byte type, byte eventType, byte[] data, byte[] messageId, byte[] sourceId, byte[]originatingPort)
	{
		super(eventType, type, data, messageId, sourceId, originatingPort);
	}
	
	/**
	 * Builder class for ResumeTransmissionMessage.
	 * @author Anshul
	 */
	public static class Builder
	{
		private byte eventType;
		
		private byte type = 18;
		
		private byte[] data;
		
		private byte[] messageId;
		
		private byte[] sourceId;
		
		private byte[] originatingPort;
		
		public Builder data(MessageData messageData) throws MessageBuildFailedException
		{
			this.data = messageData.bytes();
			return this;
		}
		
		public Builder type(byte type) {
			this.type = type;
			return this;
		}
		
		public Builder sourceId(int sourceId) {
			this.sourceId = ByteUtil.convertToByteArray(sourceId, 2);
			return this;
		}
		
		public Builder originatingPort(Integer originatingPort) {
			this.originatingPort = ByteUtil.convertToByteArray(originatingPort, 2);
			return this;
		}
		
		public ResumeTransmissionMessage build()
		{
			messageId = ByteUtil.convertToByteArray(ReservedMessageId.RESUME_MESSAGE_ID, 6);
			eventType = Integer.valueOf(0).byteValue(); 
			return new ResumeTransmissionMessage(type, eventType, data, messageId, sourceId, originatingPort);
		}
	}
}
