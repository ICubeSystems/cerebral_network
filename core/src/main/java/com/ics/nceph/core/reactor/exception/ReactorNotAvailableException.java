package com.ics.nceph.core.reactor.exception;

import com.ics.logger.NcephLogger;

public class ReactorNotAvailableException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ReactorNotAvailableException(Exception e)
	{
		NcephLogger.BOOTSTRAP_LOGGER.error(e.getMessage());
		e.printStackTrace();
	}
}
