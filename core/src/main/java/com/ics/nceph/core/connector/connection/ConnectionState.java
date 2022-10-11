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
	
	private String stateName;
	
	ConnectionState(int state, String stateName)
	{
		this.state = state;
		this.stateName = stateName;
	}
	
	public int getValue() {
		return state;
	}
	
	public String getState() {
		return stateName;
	}
	
	/**
	 * INITIATED - Newly created MessageWriter instance is in initiated state
	 */
	public static final ConnectionState AUTH_PENDING = new ConnectionState(100,"AUTH_PENDING");
	
	public static final ConnectionState PRE_READY = new ConnectionState(199,"PRE_READY");
	
	public static final ConnectionState READY = new ConnectionState(200,"READY");
	
	public static final ConnectionState AUTH_FAILED = new ConnectionState(300,"AUTH_FAILED");
	
	public static final ConnectionState TEARDOWN_REQUESTED = new ConnectionState(400,"TEARDOWN_REQUESTED");
	
	public static final ConnectionState DECOMMISIONED = new ConnectionState(500,"DECOMMISIONED");
	
}
