package com.ics.nceph.core.worker;

import com.ics.nceph.core.connector.connection.Connection;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public abstract class Writer extends Worker 
{
	public Writer(Connection connection)
	{
		super(connection);
	}
}
