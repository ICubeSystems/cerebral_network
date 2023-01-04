package com.ics.nceph.core.message;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Jan 3, 2023
 */
public class ReservedMessageId
{	
	/**
	 * Control connection bootstrap message will have message id as 0-0
	 */
	public static final long CONTROL_CONNECTION_ID = 0;
	
	/**
	 * Backpressure pause message will have message id as <node_id>-1
	 */
	public static final long PAUSE_MESSAGE_ID = 1;
	
	/**
	 * Backpressure resume message will have message id as <node_id>-2
	 */
	public static final long RESUME_MESSAGE_ID = 2;
	
	/**
	 * Event message will have message id as starting from <node_id>-3
	 */
	public static final long EVENT_MESSAGE_ID = 3;
}
