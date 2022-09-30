package com.ics.nceph.core.connector.state;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Sep 02, 2022
 */
public class ConnectorState 
{
	private int state;

	ConnectorState() {}

	ConnectorState(int state) 
	{
		this.state = state;
	}

	public int getState() 
	{
		return state;
	}

	public static final ConnectorState PENDING_AUTH = new ConnectorState(100);
	
	public static final ConnectorState READY = new ConnectorState(200);
	
	public static final ConnectorState AUTH_FAILED = new ConnectorState(300);
}
