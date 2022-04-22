package com.ics.nceph.core.reactor.exception;

import com.ics.logger.NcephLogger;

public class ImproperReactorClusterInstantiationException extends Exception 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ImproperReactorClusterInstantiationException(Exception e)
	{
		NcephLogger.BOOTSTRAP_LOGGER.error(e.getMessage());
		e.printStackTrace();
	}
}
