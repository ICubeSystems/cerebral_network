package com.ics.nceph.core.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import com.ics.nceph.core.message.exception.RelayTimeoutException;

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
	
	long lastWrittemMessageId = 0;
	
	private int relayTimeout;
	
	MessageWriterState state;
	
	// Initialize the messageCounter to 0. Increment by 1 at every message relay and reset to 0 once 256 messages are relayed
	private AtomicInteger messageCounter;
	
	public MessageWriter(int relayTimeout)
	{
		this.relayTimeout = relayTimeout;
		this.state = MessageWriterState.READY;
		this.messageCounter = new AtomicInteger(0);
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
	public void write(SocketChannel socket, Message message) throws IOException, RelayTimeoutException
	{
		// Check if the message is already sent
		if (lastWrittemMessageId != message.decoder().getId())
		{	
			// Get the message counter from the connection and set it to the message - at the receiving end it will be validated to make sure any sequence discrepancy
			message.setCounter(Integer.valueOf(messageCounter.getAndIncrement()).byteValue());
			// Get the byte buffer from the message
			ByteBuffer buffer = message.toByteBuffer();
			// if resuming write operation after the channel write timeout then start from the last buffer position
			if (state == MessageWriterState.ENGAGED)
				buffer.position(position);
			// Mark the state ENGAGED just before entering the write loop
			state = MessageWriterState.ENGAGED;
			// Try the socket.write for IO exception - as the position needs to be preserved in case of IO exception (for next write attempts - if required)
			try
			{
				long elapsed = System.currentTimeMillis();
				while (buffer.remaining() > 0) 
				{
					//write to the socket channel till the buffer is not remaining
					socket.write(buffer);
					
					elapsed = System.currentTimeMillis() - elapsed;
					// Check for the channel write timeout (). If timeout then make provision to re-send the message from the same buffer position.
					if (buffer.remaining() > 0 && elapsed > relayTimeout)
					{
						// LOG: Message [id: xxxx] write_timeout - yy bytes written, zz bytes remaining
						String logMessage = "Message [id: " + message.decoder().getId() + "] write_timeout - " + position + " bytes written, " + buffer.remaining() + " bytes remaining";
						// Break the loop - write will be attempted again in next selection (selector.select()) loop
						throw new RelayTimeoutException(new Exception(logMessage));
					}
				}
				// Once the full message is relayed, change the state of the writer to READY
				state = MessageWriterState.READY;
				// Reset the position to 0
				position = 0;
				// Record the message id of the last successfully written/ sent message
				lastWrittemMessageId = message.decoder().getId();
				// LOG: Message [id: xxxx] sent - 2189(ms)
				System.out.println("Message [id: " + lastWrittemMessageId + "] sent - " + elapsed + "ms");
			}
			catch (IOException | RelayTimeoutException e)
			{
				// Record the last written position
				position = buffer.position();
				throw e;
			}
		}
		else
			System.out.println("Message [id: " + lastWrittemMessageId + "] already sent");
	}
	
	/**
	 * This method returns true if the MessageWriter is ready to accept new message for writing
	 * 
	 * @return boolean
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 11-Jan-2022
	 */
	public boolean isReady()
	{
		return this.state.getValue() == MessageWriterState.READY.getValue() ? true : false;
	}
}