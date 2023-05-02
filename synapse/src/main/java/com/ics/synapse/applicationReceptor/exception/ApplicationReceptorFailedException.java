package com.ics.synapse.applicationReceptor.exception;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Aug 3, 2022
 */
public class ApplicationReceptorFailedException extends Exception
{
	
	/**
	 * @author Anshul
	 */
	private static final long serialVersionUID = 1L;

	public ApplicationReceptorFailedException(String message, Throwable e)
	{
		super(message, e);
	}
	
	public ApplicationReceptorFailedException(String message)
	{
		super(message);
	}
}
