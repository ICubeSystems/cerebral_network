package com.ics.nceph.core.connector.exception;

import org.apache.logging.log4j.Logger;

public class ImproperConnectorInstantiationException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ImproperConnectorInstantiationException(Exception e, Logger logger)
	{
		logger.info(e.getMessage());
		e.printStackTrace();
	}
}
