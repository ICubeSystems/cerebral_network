package com.ics.nceph.core.db.dynamoDB.exception;

/**
 * Database Exception
 * 
 * @author Chandan Verma
 * @since 19-Aug-2022
 *
 */
public class DatabaseException extends Exception
{
	private static final long serialVersionUID = 1L;

	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}
}
