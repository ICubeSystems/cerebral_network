package com.ics.nceph.core.db.document.exception;

import java.io.IOException;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 17-May-2022
 */

public class DatabaseException extends IOException
{
	
	private static final long serialVersionUID = 8870093268921144544L;

	public DatabaseException(String message) 
	{
		super(message);
	}
	
	public DatabaseException(String message, Throwable cause) 
	{
		super(message, cause);
	}
}
