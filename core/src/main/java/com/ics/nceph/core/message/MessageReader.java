package com.ics.nceph.core.message;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngineResult;

import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.SSLConnection;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
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
	MessageBuilder messageBuilder;
	
	ByteBuffer plainText;
	
	ByteBuffer encryptedDataToRead;
	
	Connection connection;
	// Initialize the messageCounter to 0. Increment by 1 at every message reception and reset to 0 once 256 messages are received
	private AtomicInteger messageCounter;
	
	public MessageReader(Connection connection)
	{
		this.connection = connection;	
		// Check if SSL mode is true then allocate encryptedDataToRead byteBuffer to read from socket
		if (NcephConstants.TLS_MODE)
			encryptedDataToRead = ByteBuffer.allocate(((SSLConnection)connection).getEncryptedDataBufferSize());
		// Allocate plainText byteBuffer to read from socket
		plainText = ByteBuffer.allocate(connection.getPlainTextBufferSize());
		
		messageBuilder = new MessageBuilder();
		
		// Initialize the messageCounter to 0. Increment by 1 at every message reception and reset to 0 once 256 messages are received
		this.messageCounter = new AtomicInteger(0);
	}
	
	/**
	 * Reads the data from the socket channel and builds the message.
	 * 
	 * @param socket
	 * @throws IOException
	 * @return void
	 * @throws ConnectionInitializationException 
	 */
	public void read() throws IOException
	{
		//TODO: use of SSL should be configurable. Call read() if SSL is disabled
		// read data from socketChannel
		if(NcephConstants.TLS_MODE) 
			readEncrypted();
		else 
		{
			plainText.clear();
			int readCount = connection.getSocket().read(plainText);
			if (readCount < 0)
				throw new SocketException("End of stream: Client connection closed properly");
			plainText.flip();
			readPlainText();
		}
		//System.out.println("Total data read in bytes: " + plainText.limit());//DEBUG
	}
	
	private void readPlainText() throws IOException 
	{
		while(plainText.position() < plainText.limit())
		{
			// 4.1 Check if the byte from the incoming buffer is the genesis byte and the state of the message builder is INITIATED
			if (messageBuilder.isInitiated() && plainText.get() == (byte)-127)
			{
				// 4.1.1 Set the read start state
				messageBuilder.startReading();
				//System.out.println("------- MESSAGE START ENCOUNTERED -------");//DEBUG
				// 4.1.2 continue the loop
				continue;
			}
			else if (messageBuilder.isReadReady())// messageBuilder is read and the header is not yet fully constructed
			{
				// Calculate the remaining header length
				int remainingHeaderLength = (NcephConstants.MESSAGE_HEADER_LENGTH-1) - messageBuilder.getTempBytesLength();
				// If the incoming buffer has the bytes to complete the message header then pass the header bytes to the messageBuilder
				if (plainText.remaining() >= remainingHeaderLength)
				{
					extractHeaderBytes(remainingHeaderLength);
					// Reset the connection message counter to 0 if 256 messages have been received (as the counter is of 1 byte in the message header).
					if(messageCounter.get() > 255)
						messageCounter.set(0);
					
					int counterCheck = messageCounter.getAndIncrement();
					// Future use: compare the counter from sender (on the message) and the receiver (on the connection)
					if (messageBuilder.messageCounter != counterCheck) 
						// Throw an exception here and handle it at the connection level. We can close the connection after cleanup if this happens (needs further thinking)
						NcephLogger.CONNECTION_LOGGER.warn("Message order broken - mId: " + messageBuilder.mId + ", Message counter: " + messageBuilder.messageCounter + ", Connection counter:" + counterCheck);
				}
				else // If the incoming buffer does not have the bytes to complete the message header then extract the header bytes and add to tempBytes
				{
					//System.out.println("------- HEADER TEMP BYTES -------");//DEBUG
					extractTempBytes();
				}
			}
			else if (messageBuilder.isHeaderAssembled())
			{
				// Calculate the remaining message data length
				int remainingDataLength = messageBuilder.messageLength - messageBuilder.getTempBytesLength();
				// If the incoming buffer has the bytes to complete the message body then create the message and reset the messageBuilder
				//System.out.println("Message body Length: " + messageBuilder.messageLength + ", remainingDataLength: " + remainingDataLength + ", Buffer Remaining:" +plainText.remaining());//DEBUG
				if (plainText.remaining() >= remainingDataLength)
				{
					// Body collection is complete - pass the data to the builder
					extractMessageBody(remainingDataLength);
					// Build the message
					Message message =  messageBuilder.build();
					NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
							.messageId(message.decoder().getId())
							.action("Read Done")
							.data(new LogData()
									.entry("type", String.valueOf(message.decoder().getType()))
									.toString())
							.logInfo());
					// Create a reader thread per message
					connection.getConnector().createPostReadWorker(message, connection);
					// Log
					NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
							.messageId(message.decoder().getId())
							.action("Read Worker Initiated")
							.data(new LogData()
									.entry("type", String.valueOf(message.decoder().getType()))
									.toString()
									)
							.logInfo());
					// Put the message in the connectors incomingMessageStore
					connection.getConnector().storeIncomingMessage(message);
					//messages.put(String.valueOf(message.decoder().getType() + message.decoder().getId()), message);
					// Reset the messageBuilder to start reading a new message
					messageBuilder.reset();
				}
				else // If the incoming buffer does not have the bytes to complete the message body then extract the data and add to tempBytes
				{
					//System.out.println("------- BODY TEMP BYTES -------");//DEBUG
					extractTempBytes();
				}
			}
		}
	}
	
	private void extractMessageBody(int remainingDataLength) throws IOException
	{
		// Collect the message body (data) from the incoming buffer into a byte array
		byte[] data = new byte[remainingDataLength];
		plainText.get(data, 0, remainingDataLength);

		// If the tempBytes are available then merge them to the newly collected body bytes before passing them to the messageBuilder
		if (messageBuilder.getTempBytesLength() > 0)
			data = ByteUtil.merge(messageBuilder.tempBytes, data);
		
		// Pass the collected bytes to messageBuilder
		messageBuilder.setData(data);
		
		// Notify the messageBuilder that the message has been full collected
		messageBuilder.bodyAssembled();
		//System.out.println("------- MESSAGE BODY ASSEMBLED");//DEBUG
	}

	private void extractTempBytes() throws IOException 
	{
		byte[] tempBytes = new byte[plainText.remaining()];
		plainText.get(tempBytes, 0, plainText.remaining());
		messageBuilder.saveTempBytes(tempBytes);
		//System.out.println("------- TEMP BYTES SAVED");//DEBUG
	}

	private void extractHeaderBytes(int remainingHeaderLength) throws IOException 
	{
		ByteBuffer headerBuffer = plainText;
		
		// If the tempBytes are available then merge them to the newly collect the remaining header bytes from the buffer and then merge the tempBytes 
		// and create a new ByteBuffer and assign it to the buffer variable
		if (messageBuilder.getTempBytesLength() > 0)
		{
			byte[] remaingHeaderBytes = new byte[remainingHeaderLength];
			plainText.get(remaingHeaderBytes, 0, remainingHeaderLength);
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
		//System.out.println("Message id:"+messageBuilder.mId + ", Message length:"+messageBuilder.messageLength + ", Message counter:"+messageBuilder.messageCounter);
	}
	
	/**
     * Will be called by the selector when the specific socket channel has data to be read.
     *
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    protected void readEncrypted() throws IOException 
    {
        encryptedDataToRead.clear();
        // read encrypted socket data to {encryptedDataToRead} byte buffer
        int bytesRead = connection.getSocket().read(encryptedDataToRead);
        if (bytesRead > 0) {
            encryptedDataToRead.flip();
            while (encryptedDataToRead.hasRemaining()) 
            {
            	plainText.clear();
            	// decrypt data from {encryptedDataToRead} to {plainText} byte buffer
                SSLEngineResult result = ((SSLConnection)connection).getEngine().unwrap(encryptedDataToRead, plainText);
                // Check the result of unwrap operation
                switch (result.getStatus()) 
                {
                	// Status OK: when the unwrap operation is completed successfully
	                case OK:
	                	// Unwrap operation might not unwrap full buffer data in 1 go, so the unwrapped data is collected in temp byte array and merged in every iteration
	                    plainText.flip();
	                    // Unwrap operation might not unwrap full buffer data in 1 go, read the data bytes decrypted in this iteration
	                    readPlainText();  
	                    //System.out.println("Incoming message: " + new String(plainText.array()));//DEBUG
	                    break;
	                 // Status BUFFER_OVERFLOW: when the plainText buffer is smaller than the data received (encryptedDataToRead)
	                case BUFFER_OVERFLOW:
	                    plainText = ((SSLConnection)connection).enlargeBuffer(plainText);
	                    break;
	                 // Status BUFFER_UNDERFLOW: when the data received (encryptedDataToRead) is smaller than the plainText buffer. 
	                 // The SSLEngine was not able to unwrap the incoming data because there were not enough source bytes available to make a complete packet.
	                case BUFFER_UNDERFLOW:
	                    encryptedDataToRead = ((SSLConnection)connection).handleBufferUnderflow(encryptedDataToRead);
	                    break;
	                // Status CLOSED: The unwrap operation just closed this side of the SSLEngine (connection closed), or the operation could not be completed because it was already closed.
	                case CLOSED:
	                	((SSLConnection)connection).closeConnection();
	                    return;
	                default:
	                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
                }
            }
        }
        // In case the other side of the connection closes then bytesRead would be -1
        else if (bytesRead < 0) 
        	((SSLConnection)connection).handleEndOfStream();
    }
}
