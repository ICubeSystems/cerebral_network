package com.ics.nceph.core.worker;

import com.ics.nceph.core.connector.connection.Connection;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public abstract class Worker extends Thread 
{
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

	public Worker(Connection connection) 
	{
		this.connection = connection;
	}
	
	public Connection getConnection() {
		return connection;
	}
	
	@Override
	public void run() 
	{
		System.out.println("Executing worker thread..............");
		//exception handling here
		execute();
	}
}
