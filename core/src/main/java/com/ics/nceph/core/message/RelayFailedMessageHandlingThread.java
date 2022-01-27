package com.ics.nceph.core.message;

import java.nio.channels.SelectionKey;

import com.ics.nceph.core.connector.connection.Connection;

/**
 * This thread is responsible for writing of partially relayed messages. 
 * <br>
 * <p>In case of a relayTimeout event due to a slow receiver, {@link MessageWriter#state} is set to engaged. 
 * The connection stops relay of any other messages. Changes the attached SelectionKey's interest to SelectionKey.OP_READ such that no further write is done on this connection.<br>
 * RelayFailedMessageHandlingThread is created and started to re attempt write after waiting for a set time by changing the 
 * connection's SelectionKey interest to SelectionKey.OP_WRITE. 
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 11-Jan-2022
 */
public class RelayFailedMessageHandlingThread extends Thread 
{
	Connection connection;
	
	Long waitBeforeWriteAgain;
	
	RelayFailedMessageHandlingThread(Long waitBeforeWriteAgain, Connection connection)
	{
		this.connection = connection;
		this.waitBeforeWriteAgain = waitBeforeWriteAgain;
	}
	
	@Override
	public void run() 
	{
		//LOG: Running RelayFailedMessageHandlingThread for message [id: xxxx]
		System.out.println("RelayFailedMessageHandlingThread..............");
		// Wait for set time and then change the interest of the connection to OP_WRITE
		try 
		{
			sleep(waitBeforeWriteAgain);
		} catch (InterruptedException e) 
		{
			//LOG: Unable to 
			e.printStackTrace();
		}
		finally
		{
			connection.setInterest(SelectionKey.OP_WRITE);
		}
	}
	
	public static class Builder
	{
		Long waitBeforeWriteAgain;
		
		Connection connection;
		
		public Builder waitBeforeWriteAgain(long waitBeforeWriteAgain)
		{
			this.waitBeforeWriteAgain = waitBeforeWriteAgain;
			return this;
		}
		
		public Builder connection(Connection connection)
		{
			this.connection = connection;
			return this;
		}
		
		public RelayFailedMessageHandlingThread build()
		{
			return new RelayFailedMessageHandlingThread(waitBeforeWriteAgain, connection);
		}
	}
}
