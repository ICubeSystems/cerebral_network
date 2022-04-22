package com.ics.nceph.core.message.type;

import java.util.HashMap;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 06-Jan-2022
 */
public abstract class MessageType 
{
	private int type;

	public static HashMap<Integer, String> messageTypes = new HashMap<Integer, String>();
	
	public MessageType(int type)
	{
		this.type = type;
	}
	
	public MessageType(int type, String className) 
	{
		this.type = type;
		messageTypes.put(type, className);
	}
	
	public int getType() {
		return type;
	}
	
	public byte getMessageType() {
		return (byte)type;
	}
	
	public static String getClassByType(int type)
	{
		return messageTypes.get(type);
	}
}