package com.ics.nceph.core.worker;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public abstract class Reader extends Worker 
{
	Message message;
	
	public Reader(Connection connection, Message message)
	{
		super(connection);
		this.message = message;
	}
	
	public Message getMessage() {
		return message;
	}

}
