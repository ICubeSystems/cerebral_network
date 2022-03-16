package com.ics.cerebrum.message.type;

import com.ics.cerebrum.receptor.PublishedEventReceptor;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.IncomingMessageType;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 06-Jan-2022
 */
public class CerebralIncomingMessageType extends IncomingMessageType
{
	CerebralIncomingMessageType(int type, Class<? extends Receptor> processorClass) 
	{
		super(type, processorClass);
	}

	/**
	 * This message type is used to publish an event in the network
	 */
	public static CerebralIncomingMessageType PUBLISH_EVENT = new CerebralIncomingMessageType(0x03, PublishedEventReceptor.class);
	
	/**
	 * 
	 */
	public static CerebralIncomingMessageType[] types = new CerebralIncomingMessageType[] {PUBLISH_EVENT};
	
	/**
	 * Returns the MessageType instance by the type supplied
	 * 
	 * @param type
	 * @throws InvalidMessageTypeException
	 * @return MessageType
	 */
	public static CerebralIncomingMessageType getMessageType(byte type) throws InvalidMessageTypeException
	{
		for (CerebralIncomingMessageType messageType : types) 
			if(messageType.getType() == ByteUtil.convertToInt(type))
				return messageType;
		throw new InvalidMessageTypeException(new Exception("Invalid message type"));
	}
}
