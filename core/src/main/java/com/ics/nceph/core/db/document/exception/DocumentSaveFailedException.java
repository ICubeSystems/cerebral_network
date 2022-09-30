package com.ics.nceph.core.db.document.exception;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 17-May-2022
 */

public class DocumentSaveFailedException extends DatabaseException
{
	private static final long serialVersionUID = 1L;
	
	public DocumentSaveFailedException(String message, Throwable cause) 
	{
		super(message, cause);
	}
}
