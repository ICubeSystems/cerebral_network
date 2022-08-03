package com.ics.nceph.config.exception;

import java.io.IOException;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 02-Aug-2022
 */

public class NodeResolutionException extends IOException
{
	private static final long serialVersionUID = 1L;
	
	public NodeResolutionException(String message, Throwable cause) 
	{
		super(message, cause);
	}
}
