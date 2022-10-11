package com.ics.synapse.exception;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 19-May-2022
 */
public class EmitException extends Exception
{
	private static final long serialVersionUID = 1L;

	public EmitException(String message, Throwable cause) {
		super(message, cause);
	}
}
