package com.ics.nceph.core.message.type;

import com.ics.nceph.core.affector.Affector;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public class OutgoingMessageType extends MessageType 
{

	private Class<? extends Affector> affectorClass;
	
	public OutgoingMessageType(int type, Class<? extends Affector> affectorClass, String typeName) 
	{
		super(type, affectorClass.getSimpleName(),typeName);
		this.affectorClass = affectorClass;
	}

	public Class<? extends Affector> getAffectorClass() {
		return affectorClass;
	}
}
