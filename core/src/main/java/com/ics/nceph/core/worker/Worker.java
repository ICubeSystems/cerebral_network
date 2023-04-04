package com.ics.nceph.core.worker;

import java.nio.channels.SocketChannel;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.reactor.Reactor;

/**
 * <p>Encephelon uses Java NIO's {@link SocketChannel} to implement non blocking TCP connections between Cerebrum and Synapse. 
 * The communication over these channels are multiplexed via {@link Reactor}. 
 * If any post processing is required to be done on the message after the socket read/ write operation, then that is done on a separate worker thread. 
 * This enables the reactor thread to focus only on socket IO tasks rather than any application tasks.</p>
 * 
 * <p>This class is a base class for all the worker threads. This class is further specialized into following 2 sub classes:
 * <ol>
 * 	<li>{@link Reader} - Thread class to handle any post operation after reading a message from the socket channel</li>
 * 	<li>{@link Writer} - Thread class to handle any post operation after writing a message to the socket channel</li>
 * </ol></p>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public abstract class Worker extends Thread implements Comparable<Worker>
{
	Message message;
	
	Connection connection;
	
	/**
	 * Abstract method
	 * 
	 * @return void
	 */
	public abstract void execute();

	public Worker(Connection connection, Message message) 
	{
		this.connection = connection;
		this.message = message;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	public Message getMessage() {
		return message;
	}
	
	@Override
	public void run() 
	{
		execute();
	}
	@Override
	public int compareTo(Worker worker)
	{
		if(getMessage().decoder().getMessageId() > worker.getMessage().decoder().getMessageId())
			return 1;
		else if(getMessage().decoder().getMessageId() == worker.getMessage().decoder().getMessageId())
			return 0;
		return -1;
	}
}
