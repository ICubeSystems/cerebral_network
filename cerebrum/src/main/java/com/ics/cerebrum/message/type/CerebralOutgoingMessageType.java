package com.ics.cerebrum.message.type;

import com.ics.cerebrum.affector.RelayedEventAffector;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.OutgoingMessageType;
import com.ics.util.ByteUtil;

public class CerebralOutgoingMessageType extends OutgoingMessageType 
{

	public CerebralOutgoingMessageType(int type, Class<? extends Affector> affectorClass) 
	{
		super(type, affectorClass);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static CerebralOutgoingMessageType RELAY_EVENT = new CerebralOutgoingMessageType(0x03, RelayedEventAffector.class);
	
	/**
	 * 
	 */
	public static CerebralOutgoingMessageType[] types = new CerebralOutgoingMessageType[] {RELAY_EVENT};
	
	/**
	 * Returns the MessageType instance by the type supplied
	 * 
	 * @param type
	 * @throws InvalidMessageTypeException
	 * @return MessageType
	 */
	public static CerebralOutgoingMessageType getMessageType(byte type) throws InvalidMessageTypeException
	{
		for (CerebralOutgoingMessageType messageType : types) 
			if(messageType.getType() == ByteUtil.convertToInt(type))
				return messageType;
		throw new InvalidMessageTypeException(new Exception("Invalid message type"));
	}

}
