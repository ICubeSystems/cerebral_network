package com.ics.nceph.core.message;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.util.ByteUtil;

/**
 * This class is responsible for reading the incoming bytes and constructing the messages. This class is a singleton implementation per {@link Connection} instance.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 05-Jan-2022
 */
public class MessageReader 
{
	private ConcurrentHashMap<Long, Message> messages;
	
	MessageBuilder messageBuilder;
	
	ByteBuffer buffer;
	
	// Initialize the messageCounter to 0. Increment by 1 at every message reception and reset to 0 once 256 messages are received
	private AtomicInteger messageCounter;
	
	public MessageReader()
	{
		messageBuilder = new MessageBuilder();
		messages = new ConcurrentHashMap<Long, Message>();
		// Allocate a byteBuffer to read from socket
		buffer = ByteBuffer.allocate(NcephConstants.READER_BUFFER_SIZE);
		// Initialize the messageCounter to 0. Increment by 1 at every message reception and reset to 0 once 256 messages are received
		this.messageCounter = new AtomicInteger(0);
	}
	
	/**
	 * Reads the data from the socket channel and builds the message.
	 * 
	 * @param socket
	 * @throws IOException
	 * @return void
	 */
	public void read(SocketChannel socket) throws IOException
	{
		// 1. Clear the buffer for the new read from the socket
		buffer.clear();
		// 2. Read from socketChannel in to the buffer
		int readCount = socket.read(buffer);
		// socket.read returns -1 if the socket is closed properly by the client by calling socket.close. Throw SocketException 
		if (readCount < 0)
			throw new SocketException("Client connection closed properly");
		//System.out.println("received::: " + new String(buffer.array()));
		// 3. Set  limit to current position & the position to 0
		buffer.flip();
		// 4. Loop over the buffer till the limit (end of data in the buffer)
		while(buffer.position() < buffer.limit())
		{
			// 4.1 Check if the byte from the incoming buffer is the genesis byte and the state of the message builder is INITIATED
			if (messageBuilder.isInitiated() && buffer.get() == (byte)-127)
			{
				// 4.1.1 Set the read start state
				messageBuilder.startReading();
				//System.out.println("------- MESSAGE START ENCOUNTERED -------");
				// 4.1.2 continue the loop
				continue;
			}
			else if (messageBuilder.isReadReady())// messageBuilder is read and the header is not yet fully constructed
			{
				// Calculate the remaining header length
				int remainingHeaderLength = (NcephConstants.MESSAGE_HEADER_LENGTH-1) - messageBuilder.getTempBytesLength();
				// If the incoming buffer has the bytes to complete the message header then pass the header bytes to the messageBuilder
				if (buffer.remaining() >= remainingHeaderLength)
				{
					extractHeaderBytes(remainingHeaderLength);
					// Future use: compare the counter from sender (on the message) and the receiver (on the connection)
					if (messageBuilder.messageCounter != messageCounter.getAndIncrement())
						// Throw an exception here and handle it at the connection level. We can close the connection after cleanup if this happens (needs further thinking)
						System.out.println("******* MESSAGE ORDER BROKEN *******");
				}
				else // If the incoming buffer does not have the bytes to complete the message header then extract the header bytes and add to tempBytes
				{
					//System.out.println("------- HEADER TEMP BYTES -------");
					extractTempBytes();
				}
			}
			else if (messageBuilder.isHeaderAssembled())
			{
				// Calculate the remaining message data length
				int remainingDataLength = messageBuilder.messageLength - messageBuilder.getTempBytesLength();
				// If the incoming buffer has the bytes to complete the message body then create the message and reset the messageBuilder
				//System.out.println("Message body Length: " + messageBuilder.messageLength + ", remainingDataLength: " + remainingDataLength + ", Buffer Remaining:" +buffer.remaining());
				if (buffer.remaining() >= remainingDataLength)
				{
					// Body collection is complete - pass the data to the builder
					extractMessageBody(remainingDataLength);
					// Build the message
					messages.put(messageBuilder.mId, messageBuilder.build());
					// Reset the messageBuilder to start reading a new message
					messageBuilder.reset();
				}
				else // If the incoming buffer does not have the bytes to complete the message body then extract the data and add to tempBytes
				{
					//System.out.println("------- BODY TEMP BYTES -------");
					extractTempBytes();
				}
			}
		}
	}
	
	public ConcurrentHashMap<Long, Message> getMessages() 
	{
		return messages;
	}

	
	private void extractMessageBody(int remainingDataLength) throws IOException
	{
		// Collect the message body (data) from the incoming buffer into a byte array
		byte[] data = new byte[remainingDataLength];
		buffer.get(data, 0, remainingDataLength);

		// If the tempBytes are available then merge them to the newly collected body bytes before passing them to the messageBuilder
		if (messageBuilder.getTempBytesLength() > 0)
			data = ByteUtil.merge(messageBuilder.tempBytes, data);
		
		// Pass the collected bytes to messageBuilder
		messageBuilder.setData(data);
		
		// Notify the messageBuilder that the message has been full collected
		messageBuilder.bodyAssembled();
		//System.out.println("------- MESSAGE BODY ASSEMBLED");
	}

	private void extractTempBytes() throws IOException 
	{
		byte[] tempBytes = new byte[buffer.remaining()];
		buffer.get(tempBytes, 0, buffer.remaining());
		messageBuilder.saveTempBytes(tempBytes);
		//System.out.println("------- TEMP BYTES SAVED");
	}

	private void extractHeaderBytes(int remainingHeaderLength) throws IOException 
	{
		ByteBuffer headerBuffer = buffer;
		
		// If the tempBytes are available then merge them to the newly collect the remaining header bytes from the buffer and then merge the tempBytes 
		// and create a new ByteBuffer and assign it to the buffer variable
		if (messageBuilder.getTempBytesLength() > 0)
		{
			byte[] remaingHeaderBytes = new byte[remainingHeaderLength];
			buffer.get(remaingHeaderBytes, 0, remainingHeaderLength);
			headerBuffer = ByteBuffer.wrap(ByteUtil.merge(messageBuilder.tempBytes, remaingHeaderBytes));
		}
		
		// set counter
		messageBuilder.setCounter(headerBuffer.get());
		// set flags
		messageBuilder.setFlags(headerBuffer.get());
		// set type
		messageBuilder.setType(headerBuffer.get());
		// set sourceId
		byte[] sourceId = new byte[2];
		headerBuffer.get(sourceId, 0, 2);
		messageBuilder.setSourceId(sourceId);
		// set messageId
		byte[] messageId = new byte[6];
		headerBuffer.get(messageId, 0, 6);
		messageBuilder.setMessageId(messageId);
		// set dataLength
		byte[] dataLength = new byte[4];
		headerBuffer.get(dataLength, 0, 4);
		messageBuilder.setDataLength(dataLength);
		
		// Notify the messageBuilder that the header collection is complete
		messageBuilder.headerAssembled();
		//System.out.println("------- MESSAGE HEADER ASSEMBLED -- "+ messageBuilder.mId);
	}
}
