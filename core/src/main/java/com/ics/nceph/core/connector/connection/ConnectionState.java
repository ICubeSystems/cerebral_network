package com.ics.nceph.core.connector.connection;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 13-Jan-2022
 */
public class ConnectionState 
{
	private int state;
	
	ConnectionState(int state)
	{
		this.state = state;
	}
	
	public int getValue() {
		return state;
	}
	
	/**
	 * INITIATED - Newly created MessageWriter instance is in initiated state
	 */
	public static final ConnectionState AUTH_PENDING = new ConnectionState(100);
	
	public static final ConnectionState READY = new ConnectionState(200);
	
	public static final ConnectionState AUTH_FAILED = new ConnectionState(300);
	
	public static final ConnectionState TEARDOWN_REQUESTED = new ConnectionState(400);
	
	public static final ConnectionState DECOMMISIONED = new ConnectionState(500);
	
}
