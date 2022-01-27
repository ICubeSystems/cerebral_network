package com.ics.nceph.core.reactor;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;

/**
 * <p>This class runs the NIO selector event loop. </p>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 20-Dec-2021
 */
public class Reactor extends Thread	
{
	Integer reactorId;
	
	Selector selector;
	
	/**
	 * 
	 * @param id
	 * @throws IOException
	 */
	public Reactor(Integer id) throws IOException
	{
		// TODO exception handling - ReactorInstantiationException
		this.reactorId = id;
		selector = Selector.open();
	}
	
	@Override
	public void run() 
	{
		// TODO: Provision for health check of reactor threads
		// TODO: Separate log files per reactor thread
		System.out.println("Reactor "+ getReactorId() + " now running####");
		// 1. Run an endless loop
		while (true) 
		{
			//System.out.println("Reactor "+ getReactorId() + " running....");
			try 
			{
				int readyKeys = selector.select();
				System.out.println("Reactor "+ getReactorId() + ": Number of channels ready:" + readyKeys);
				
				// Get an iterator over the set of selected keys
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                
                // Look at each key in the selected set
                while (keys.hasNext()) 
                {
                	SelectionKey key = keys.next();
                	
                	// Remove the key from the selectedKeys set
                	keys.remove();
                	
                	// If the key is for accept operation (connection request)
                	if (key.isAcceptable()) 
                	{
                		Connector connector = (Connector) key.attachment();
                		connector.acceptConnection();
                	}
                	else if (key.isReadable()) 
                	{
                		System.out.println("Reactor "+ getReactorId() + ": Reading...");
                		Connection connection = (Connection) key.attachment();
                		connection.read();
                	}
                	else if (key.isWritable()) 
                	{
                		System.out.println("Reactor "+ getReactorId() + ": Writing...");
                		Connection connection = (Connection) key.attachment();
                		connection.write();
                	}
                }
			} catch (IOException | ImproperReactorClusterInstantiationException | ReactorNotAvailableException e) 
			{
				e.printStackTrace();
				try {
					Thread.sleep(2*60*1000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			} 
		}
	}
	
	/**
	 * Return the id of the reactor instance
	 * 
	 * @return Integer
	 */
	public Integer getReactorId() 
	{
		return reactorId;
	}
	
	/**
	 * This method returns the {@link Selector} instance attached to the reactor
	 * 
	 * @return Selector
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 22-Dec-2021
	 */
	public Selector getSelector() 
	{
		return selector;
	}
	
	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 20-Dec-2021
	 */
	public static class Builder
	{
		Integer reactorId;
		
		Integer maxPoolSize;
		
		public Builder reactorId(Integer reactorId) {
			this.reactorId = reactorId;
			return this;
		}
		
		public Builder maxWorkerPoolSize(Integer maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
			return this;
		}
	}
}
