package com.ics.nceph.core.message.type;

import com.ics.nceph.core.receptor.Receptor;

public class IncomingMessageType extends MessageType 
{
	private Class<? extends Receptor> processorClass;
	
	public IncomingMessageType(int type, Class<? extends Receptor> processorClass) 
	{
		super(type, processorClass.getSimpleName());
		this.processorClass = processorClass;
	}
	
	public Class<? extends Receptor> getProcessorClass() {
		return processorClass;
	}
}
