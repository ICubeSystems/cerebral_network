package com.ics.synapse.exception;

import com.ics.synapse.ncephEvent.Event;

/**
 * Exception thrown if there is any problem in constructing {@link Event} object for emission
 * 
 * @author Anshul
 * @version 1.0.1 
 * @since Apr 24, 2023
 */
public class EventDataException extends Exception
{
	private static final long serialVersionUID = 1L;

	public EventDataException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public EventDataException(String message) {
		super(message);
	}
}

