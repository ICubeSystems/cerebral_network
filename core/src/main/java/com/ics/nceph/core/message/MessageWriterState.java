package com.ics.nceph.core.message;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 11-Jan-2022
 */
public class MessageWriterState 
{
	private int state;
	
	MessageWriterState(int state)
	{
		this.state = state;
	}
	
	public int getValue() {
		return state;
	}
	
	/**
	 * INITIATED - Newly created MessageWriter instance is in initiated state
	 */
	public static MessageWriterState READY = new MessageWriterState(100);
	
	/**
	 * RELAY_STARTED - 
	 */
	public static MessageWriterState ENGAGED = new MessageWriterState(200);
	
	/**
	 * RELAY_STARTED - 
	 */
	public static MessageWriterState TERMINATED = new MessageWriterState(300);
}
