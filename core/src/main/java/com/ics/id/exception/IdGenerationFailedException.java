package com.ics.id.exception;

import java.io.IOException;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 17-May-2022
 */

public class IdGenerationFailedException extends IOException
{
	private static final long serialVersionUID = 1L;
	
	public IdGenerationFailedException(String message, Throwable cause) 
	{
		super(message, cause);
	}
	
	public IdGenerationFailedException(String message) {
		super(message);
	}
}
