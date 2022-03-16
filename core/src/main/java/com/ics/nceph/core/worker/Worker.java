package com.ics.nceph.core.worker;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public abstract class Worker extends Thread 
{
	Message message;
	
	Connection connection;
	
	/**
	 * Abstract method
	 * 
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 22-Dec-2021
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
		//System.out.println("Executing worker thread..............");
		//exception handling here
		execute();
	}
}
