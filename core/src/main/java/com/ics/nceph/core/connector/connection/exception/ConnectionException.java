package com.ics.nceph.core.connector.connection.exception;

public class ConnectionException extends Exception 
{
	private static final long serialVersionUID = -7054284488123083980L;

	public ConnectionException(Exception e)
	{
		e.printStackTrace();
	}
}
