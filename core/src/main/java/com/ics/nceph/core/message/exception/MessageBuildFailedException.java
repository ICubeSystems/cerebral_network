package com.ics.nceph.core.message.exception;

import java.io.IOException;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 17-May-2022
 */
public class MessageBuildFailedException extends IOException
{
	private static final long serialVersionUID = 1L;

	public MessageBuildFailedException(String message, Throwable cause) 
	{
		super(message, cause);
	}
}
