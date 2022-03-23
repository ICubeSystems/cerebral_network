package com.ics.nceph.core.ssl.exception;

/**
 * 
 * @author Anshul
 * @since 15-Mar-2022
 */
public class SSLHandshakeException extends Exception 
{
	
	private static final long serialVersionUID = 1L;
	
	public SSLHandshakeException(String message) 
	{
		super(message);
	}
}
