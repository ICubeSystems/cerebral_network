package com.ics.synapse.applicationReceptor.exception;

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
}
