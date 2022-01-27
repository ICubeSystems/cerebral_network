package com.ics.nceph.core.message.exception;

public class InvalidMessageTypeException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public InvalidMessageTypeException(Exception e)
	{
		e.printStackTrace();
	}
}
