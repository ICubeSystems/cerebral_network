package com.ics.cerebrum.configuration.exception;

/**
 * Configuration Exception
 * @author Anshul
 * @version 1.0
 * @since Sep 28, 2022
 */
public class ConfigurationException extends Exception
{

	
	private static final long serialVersionUID = 757644605856267671L;

	public ConfigurationException(Exception e)
	{
		e.printStackTrace();
	}
	
	public ConfigurationException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
