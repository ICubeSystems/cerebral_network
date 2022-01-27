package com.ics.nceph.core.connector;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 13-Jan-2022
 */
public class ConnectorType 
{
	private int type;
	
	ConnectorType(int type)
	{
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	
	/**
	 * Server
	 */
	public static final ConnectorType CEREBRAL = new ConnectorType(1000);
	
	/**
	 * Client
	 */
	public static final ConnectorType SYNAPTIC = new ConnectorType(2000);

}
