package com.ics.nceph.core.message;

import java.io.IOException;
import java.util.Date;

import com.ics.util.ByteUtil;

/**
 * This class holds the bytes of a message and then builds a message object from them.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 01-Jan-2022
 */
public class MessageBuilder 
{
	/**
	 * States of the builder:
	 * 1. Pending read - Newly created MessageBuilder instance is in initiated state or when the next genesis byte is encountered after a successful message building
	 * 2. Header assembled - Once the header is fully assembled. If the receiving buffer has less than 16 bytes remaining after the genesis byte then the bytes are held in temporary bytes instead of message bytes
	 * 3. Body assembled - Once the full body is received. If the receiving buffer has less bytes than the length of the message then the data bytes are held in the temporary bytes instead of body bytes on the message 
	 */
	
	private IORecord.Builder readRecordBuilder;
	
	public int state;
	
	public int messageLength;
	
	public long mId;
	
	public int messageCounter;
	
	byte[] tempBytes;
	
	byte counter;
	
	byte flags;
	
	byte type;
	
	byte[] sourceId = new byte[2];
	
	byte[] messageId = new byte[6];
	
	byte[] dataLength = new byte[4];
	
	byte[] data;
	
	MessageBuilder()
	{
		state = 100;
	}
	
	public void setCounter(byte counter) {
		this.counter = counter;
	}

	public void setFlags(byte flags) {
		this.flags = flags;
	}

	public void setType(byte type) {
		this.type = type;
	}

	public void setSourceId(byte[] sourceId) {
		this.sourceId = sourceId;
	}

	public void setMessageId(byte[] messageId) {
		this.messageId = messageId;
	}

	public void setDataLength(byte[] dataLength) {
		this.dataLength = dataLength;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public void saveTempBytes(byte[] tempBytes) throws IOException 
	{
		// If the tempBytes is set to null then assign the incoming bytes else merge and then assign
		this.tempBytes = (this.tempBytes == null) ? tempBytes : ByteUtil.merge(this.tempBytes, tempBytes);
	}
	
	/**
	 * Mark the state of the message builder to HEADER_ASSEMBLED and reset the tempBytes. This method should be called when the message header is fully collected.
	 * 
	 * @return void
	 */
	public void headerAssembled()
	{
		// Set the state to HEADER_ASSEMBLED
		this.state = MessageBuilderState.HEADER_ASSEMBLED.getValue();
		
		// Reset the tempBytes
		this.tempBytes = null;
		
		// Set the length of the message data in integer
		this.messageLength = ByteUtil.convertToInt(dataLength);
		
		// Set the id of the message in long
		this.mId = ByteUtil.convertToLong(messageId);
		
		// Set the messageCounter
		this.messageCounter = ByteUtil.convertToInt(counter);
	}
	
	/**
	 * Mark the state of the message builder to BODY_ASSEMBLED and reset the tempBytes. This method should be called when the message is fully collected.
	 * 
	 * @return void
	 */
	public void bodyAssembled()
	{
		// Set the state to BODY_ASSEMBLED
		this.state = MessageBuilderState.BODY_ASSEMBLED.getValue();
		
		// Reset the tempBytes
		this.tempBytes = null;
	}
	
	/**
	 * This method resets the MessageBuilder to start reading a new message
	 * 
	 * @return void
	 */
	public void reset()
	{
		// Set the state to INITIATED
		this.state = MessageBuilderState.INITIATED.getValue();
		
		// Reset the variables
		this.tempBytes = null;
		this.mId = 0;
		this.messageLength = 0;
		this.readRecordBuilder = null;
	}
	
	public void startReading()
	{
		this.state = MessageBuilderState.READ_STARTED.getValue();
		this.readRecordBuilder = new IORecord.Builder().start(new Date());
	}
	
	public boolean isReadReady()
	{
		return this.state == MessageBuilderState.READ_STARTED.getValue() ? true : false;
	}
	
	public boolean isInitiated()
	{
		return this.state == MessageBuilderState.INITIATED.getValue() ? true : false;
	}
	
	public boolean isHeaderAssembled()
	{
		return this.state == MessageBuilderState.HEADER_ASSEMBLED.getValue() ? true : false;
	}
	
	public int getTempBytesLength()
	{
		return (tempBytes == null) ? 0 : tempBytes.length;
	}
	
	public Message build()
	{
		return new Message(
				counter, 
				flags, 
				type, 
				sourceId, 
				messageId, 
				dataLength, 
				data, 
				this.readRecordBuilder.end(new Date()).build()
				);
	}
}
