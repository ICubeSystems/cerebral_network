package com.ics.nceph.core.event.exception;

import org.apache.logging.log4j.Logger;

public class EventNotSubscribedException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public EventNotSubscribedException(Exception e, Logger logger)
	{
		logger.info(e.getMessage());
		e.printStackTrace();
	}
}
