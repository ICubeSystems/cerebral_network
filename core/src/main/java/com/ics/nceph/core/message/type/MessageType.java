package com.ics.nceph.core.message.type;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 06-Jan-2022
 */
public class MessageType 
{
	private int type;
	
	public MessageType(int type)
	{
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
}