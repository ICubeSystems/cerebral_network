package com.ics.nceph.core.event.exception;

public class ImproperEventBufferInstantiationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ImproperEventBufferInstantiationException(Exception e)
	{
		//logger.info(e.getMessage());
		e.printStackTrace();
	}
}
