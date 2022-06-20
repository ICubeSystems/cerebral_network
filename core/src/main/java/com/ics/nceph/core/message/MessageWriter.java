package com.ics.nceph.core.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;

import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.SSLConnection;
import com.ics.nceph.core.message.exception.RelayTimeoutException;
import com.ics.nceph.core.message.type.MessageType;
import com.ics.util.ByteUtil;

/**
 * This class is responsible for writing/ relaying the queued up messages. This class is a singleton implementation per Connection instance.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 05-Jan-2022
 */
public class MessageWriter 
{
	int position = 0;
	
	String lastWrittemMessageId = null;
	
	private int relayTimeout;
	
	MessageWriterState state;
	
	ByteBuffer plainText;
	
	ByteBuffer encryptedData;
	
	Connection connection;
	// Initialize the messageCounter to 0. Increment by 1 at every message relay and reset to 0 once 256 messages are relayed
	private AtomicInteger messageCounter;
	
	public MessageWriter(Connection connection)
	{
		this.connection = connection;	
		
		if(NcephConstants.TLS_MODE) 
	        encryptedData = ByteBuffer.allocate(((SSLConnection)connection).getEncryptedDataBufferSize());
		
		this.relayTimeout = connection.getRelayTimeout();
		this.state = MessageWriterState.READY;
		this.messageCounter = new AtomicInteger(0);
		
	}
	public void sslWrite() throws IOException 
	{
        // Every wrap call will remove 16KB from the original message and send
        encryptedData.clear();
        // Encrypt plain text of (plainText) to (encryptedData).
        SSLEngineResult result = ((SSLConnection)connection).getEngine().wrap(plainText, encryptedData);
        switch (result.getStatus()) 
        {
        	// Status OK: when the wrap operation is completed successfully
        	case OK:
	            encryptedData.flip();
	            while (encryptedData.hasRemaining()) {
	            	// Write encrypted data to socket. 
						connection.getSocket().write(encryptedData);
	            }
	            break;
	        // Status BUFFER_OVERFLOW: when the encryptedData is smaller than the plainText.
	        case BUFFER_OVERFLOW:
	            encryptedData = ((SSLConnection)connection).enlargeBuffer(encryptedData);
	            break;
	        // Status BUFFER_UNDERFLOW: when the plainText buffer is smaller than the encryptedData. 
	        case BUFFER_UNDERFLOW:
	            throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here");
	        // Status CLOSED: The unwrap operation just closed this side of the SSLEngine (connection closed), or the operation could not be completed because it was already closed.
	        case CLOSED:
	        	((SSLConnection)connection).closeConnection();
	            return;
	        default:
	            throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
        }
	}

	/**
	 * This method writes/ relays the Message to the socket channel. If the receiver is slow then write operation is repeated till the message is fully written. 
	 * The write operation times out after trying for relayTimeout milliseconds and throws {@link RelayTimeoutException}.
	 * 
	 * @param socket to relay the message
	 * @param message to be relayed
	 * @throws IOException - in case there is any IO exception on the socket
	 * @throws RelayTimeoutException - The write operation times out after trying for relayTimeout milliseconds
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 12-Jan-2022
	 */
	public void write(Message message) throws IOException, RelayTimeoutException
	{
		//log
		NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
				.messageId(message.decoder().getId())
				.action("Write start")
				.data(new LogData()
						.entry("connectionId", String.valueOf(connection.getId()))
						.entry("messageType", MessageType.getNameByType(message.decoder().getType()))
						.toString()
						)
				.logInfo());
		// Get the message counter from the connection and set it to the message - at the receiving end it will be validated to make sure any sequence discrepancy
		int msgCounter = messageCounter.getAndIncrement(); 
		message.setCounter(Integer.valueOf(msgCounter).byteValue());

		message.setTimeStamp(ByteUtil.convertToByteArray(new Date().getTime(), Long.SIZE/8));

		// Get the byte buffer from the message
		if(plainText!=null)
			plainText.clear();

		plainText = message.toByteBuffer();
		// if resuming write operation after the channel write timeout then start from the last buffer position
		if (state == MessageWriterState.ENGAGED)
			plainText.position(position);
		// Mark the state ENGAGED just before entering the write loop
		state = MessageWriterState.ENGAGED;
		// Try the socket.write for IO exception - as the position needs to be preserved in case of IO exception (for next write attempts - if required)
		try
		{
			IORecord.Builder writeRecord = new IORecord.Builder().start(new Date().getTime());
			long elapsed = 0;
			while (plainText.remaining() > 0) 
			{
				elapsed = System.currentTimeMillis() - elapsed;
				//write to the socket channel till the buffer is not remaining
				try 
				{
					if(NcephConstants.TLS_MODE) 
						sslWrite();
					else 
						connection.getSocket().write(plainText);
				} catch (IOException e) 
				{
					// Log
					NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
							.messageId(message.decoder().getId())
							.action("Write Error")
							.data(new LogData()
									.entry("connectionId", String.valueOf(connection.getId()))
									.entry("messageType", MessageType.getNameByType(message.decoder().getType()))
									.toString()
									)
							.logError(),e);
					throw e;
				}

				elapsed = System.currentTimeMillis() - elapsed;
				// Check for the channel write timeout (). If timeout then throw RelayTimeoutException and break the write loop. 
				// In the calling method RelayTimeoutException is caught and a new thread (RelayFailedMessageHandlingThread) is started which will set the interest to write again after specified delay.
				// In case any other reader thread changes the interest of this connection to write then this code will again be executed and it will try resend the message from the last position. If successfully done then it will send the new message. In this case the RelayFailedMessageHandlingThread will be rendered useless as the relay queue of this conection will already be empty.
				if (plainText.remaining() > 0 && elapsed > relayTimeout)
				{
					// LOG: Message [id: xxxx] write_timeout - yy bytes written, zz bytes remaining
					String logMessage = "Message [id: " + message.decoder().getId() + ", type: " + message.decoder().getType() + "] write_timeout - " + position + " bytes written, " + plainText.remaining() + " bytes remaining";
					// Break the loop - write will be attempted again in next selection (selector.select()) loop
					throw new RelayTimeoutException(new Exception(logMessage));
				}
			}
			// Set the write record - may be required in the write worker thread
			writeRecord.end(new Date().getTime());
			message.setWriteRecord(writeRecord.build());

			// Once the full message is relayed, change the state of the writer to READY
			state = MessageWriterState.READY;
			// Reset the position to 0
			position = 0;
			// Record the message id of the last successfully written/ sent message
			lastWrittemMessageId = message.decoder().getId();
			
			// Remove the message from the relayQueue & Store message sent to the outgoing message register
			connection.getRelayQueue().poll();
			connection.getConnector().removeConnectionQueuedUpMessage(message);
			connection.getConnector().storeOutgoingMessage(message);
			// Open a write thread to do the post writing work like updating the ACK status of the messages
			connection.getConnector().createPostWriteWorker(message, connection);
			// Log
			NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
					.messageId(message.decoder().getId())
					.action("Write Worker Initiated")
					.data(new LogData()
							.entry("messageType", MessageType.getNameByType(message.decoder().getType()))
							.entry("workerClass", MessageType.getClassByType(message.decoder().getType()))
							.toString()
							)
					.logInfo());
		}
		catch (RelayTimeoutException e)
		{
			// Record the last written position
			position = plainText.position();
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(message.decoder().getId())
					.action("Write Timeout")
					.data(new LogData()
							.entry("connectionId", String.valueOf(connection.getId()))
							.entry("messageType", MessageType.getNameByType(message.decoder().getType()))
							.toString()
							)
					.logError(),e);
			throw e;
		}
		//log
		NcephLogger.MESSAGE_LOGGER.info(new MessageLog.Builder()
				.messageId(message.decoder().getId())
				.action("Write done")
				.data(new LogData()
						.entry("messageType", MessageType.getNameByType(message.decoder().getType()))
						.entry("connectionId", String.valueOf(connection.getId()))
						.toString()
						)
				.logInfo());
	}
	
	/**
	 * This method returns true if the MessageWriter is ready to accept new message for writing
	 * 
	 * @return boolean
	 */
	public boolean isReady()
	{
		return this.state.getValue() == MessageWriterState.READY.getValue() ? true : false;
	}
}
