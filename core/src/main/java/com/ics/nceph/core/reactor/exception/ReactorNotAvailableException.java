package com.ics.nceph.core.reactor.exception;

import org.apache.logging.log4j.Logger;

public class ReactorNotAvailableException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ReactorNotAvailableException(Exception e, Logger logger)
	{
		logger.info(e.getMessage());
		e.printStackTrace();
	}
}
