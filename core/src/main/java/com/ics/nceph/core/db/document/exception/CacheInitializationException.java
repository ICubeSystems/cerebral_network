package com.ics.nceph.core.db.document.exception;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Sep 27, 2022
 */
public class CacheInitializationException extends Exception
{
	private static final long serialVersionUID = 4344214530999911781L;

	public CacheInitializationException(String message)
	{
		super(message);
	}
	
	public CacheInitializationException(String message, Throwable exception)
	{
		super(message, exception);
	}
}
