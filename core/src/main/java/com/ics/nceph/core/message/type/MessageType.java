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
	//TODO add a new attribute as name
	
	private int type;
	
	public static HashMap<Integer, String> className = new HashMap<Integer, String>();
	
	public static HashMap<Integer, String> typeName = new HashMap<Integer, String>();
	
	public static HashMap<Integer, MessageClassification> classificationMap = new HashMap<Integer, MessageClassification>();
	
	public MessageType(int type)
	{
		this.type = type;
	}
	
	public MessageType(int type, String messageClass, String name, MessageClassification classification) 
	{
		this.type = type;
		className.put(type, messageClass);
		typeName.put(type, name);
		classificationMap.put(type, classification);
	}
	
	public int getType() {
		return type;
	}
	
	public byte getMessageType() {
		return (byte)type;
	}
	
	public static String getClassByType(int type)
	{
		return className.get(type);
	}
	
	public static String getNameByType(int type)
	{
		return typeName.get(type);
	}
	
	public static MessageClassification getclassificationByType(int type)
	{
		return classificationMap.get(type);
	}
}