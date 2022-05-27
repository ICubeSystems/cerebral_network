package com.ics.nceph.core.connector.connection.exception;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 17-May-2022
 */

public class AuthenticationFailedException extends Exception
{
	private static final long serialVersionUID = 1L;

	public AuthenticationFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
