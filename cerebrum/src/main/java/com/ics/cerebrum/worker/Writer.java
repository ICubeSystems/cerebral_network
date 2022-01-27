package com.ics.cerebrum.worker;

import java.nio.channels.SelectionKey;
import java.util.ArrayList;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.worker.Worker;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public class Writer extends Worker 
{
	public Writer(Connection connection)
	{
		super(connection);
	}

	@Override
	public void execute() 
	{
		//try 
		//{
			// 1. Get the events to publish from the event queue attached to the connection
			ArrayList<Message> messages = getConnection().dequeueMessages();
			
			// 2. Loop over the events and write to the Socket
			for (Message message : messages) 
			{
				// 2.1 Create the byte buffer for the event
				//EventBuffer buffer = new EventBuffer.Builder().event(event).build();
				// 2.2 Write to the socket channel till the buffer has bytes remaining
				//while (buffer.getBuffer().hasRemaining())
				//	connection.getSocket().write(buffer.getBuffer());
			}
			
			// 3. Set the connnection's socket channel interest to read
			getConnection().setInterest(SelectionKey.OP_READ);
			
		//} //catch (IOException | ImproperEventBufferInstantiationException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
	}
}
