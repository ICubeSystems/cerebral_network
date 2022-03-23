package com.ics.nceph.core.connector.connection.exception;

/**
 * 
 * @author Anshul
 * @since 17-Mar-2022
 */
public class ConnectionInitializationException extends Exception 
{
	
	private static final long serialVersionUID = 1L;
	
	public ConnectionInitializationException(String message, Throwable cause) 
	{
		super(message, cause);
		
	}
}
