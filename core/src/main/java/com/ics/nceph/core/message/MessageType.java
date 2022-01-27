package com.ics.nceph.core.message;

import com.ics.nceph.core.receptor.Receptor;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 06-Jan-2022
 */
public class MessageType 
{
	private int type;
	
	private Class<? extends Receptor> processorClass;
	
	public MessageType(int type, Class<? extends Receptor> processorClass)
	{
		this.type = type;
		this.processorClass = processorClass;
	}
	
	public int getType() {
		return type;
	}

	public Class<? extends Receptor> getProcessorClass() {
		return processorClass;
	}
}