package com.ics.nceph.core.document.exception;

import java.io.IOException;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 17-May-2022
 */

public class DocumentSaveFailedException extends IOException
{
	private static final long serialVersionUID = 1L;
	
	public DocumentSaveFailedException(String message, Throwable cause) 
	{
		super(message, cause);
	}
}
