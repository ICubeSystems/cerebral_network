package com.ics.synapse.message.type;

import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.IncomingMessageType;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.receptor.RelayedEventReceptor;
import com.ics.util.ByteUtil;

public class SynapticIncomingMessageType extends IncomingMessageType
{

	public SynapticIncomingMessageType(int type, Class<? extends Receptor> processorClass) 
	{
		super(type, processorClass);
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static SynapticIncomingMessageType RELAY_EVENT = new SynapticIncomingMessageType(0x03, RelayedEventReceptor.class);
	
	/**
	 * 
	 */
	public static SynapticIncomingMessageType[] types = new SynapticIncomingMessageType[] {RELAY_EVENT};
	
	/**
	 * Returns the MessageType instance by the type supplied
	 * 
	 * @param type
	 * @throws InvalidMessageTypeException
	 * @return MessageType
	 */
	public static SynapticIncomingMessageType getMessageType(byte type) throws InvalidMessageTypeException
	{
		for (SynapticIncomingMessageType messageType : types) 
			if(messageType.getType() == ByteUtil.convertToInt(type))
				return messageType;
		throw new InvalidMessageTypeException(new Exception("Invalid message type"));
	}
}
