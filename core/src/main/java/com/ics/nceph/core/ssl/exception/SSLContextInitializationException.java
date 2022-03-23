package com.ics.nceph.core.ssl.exception;

/**
 * 
 * @author Anshul
 * @since 15-Mar-2022
 */
public class SSLContextInitializationException extends Exception 
{
	
	private static final long serialVersionUID = 1L;
	
	public SSLContextInitializationException(String message, Throwable cause) 
	{
		super(message, cause);
	}
}
