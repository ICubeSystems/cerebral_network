package com.ics.nceph.core.message.exception;

/**
 * This Exception is thrown when the receiver is slow and the write operation times out after trying for the set relayTimeout milliseconds
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 12-Jan-2022
 */
public class RelayTimeoutException extends Exception
{
	private static final long serialVersionUID = 1L;
	
	public RelayTimeoutException(Exception e)
	{
		System.out.println(e.getMessage());
		e.printStackTrace();
	}
}
