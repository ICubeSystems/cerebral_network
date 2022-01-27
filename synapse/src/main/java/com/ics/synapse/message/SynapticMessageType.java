package com.ics.synapse.message;

import com.ics.nceph.core.message.MessageType;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.receptor.IngestEventReceptor;
import com.ics.util.ByteUtil;

public class SynapticMessageType extends MessageType
{

	public SynapticMessageType(int type, Class<? extends Receptor> processorClass) 
	{
		super(type, processorClass);
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static SynapticMessageType RELAY_EVENT = new SynapticMessageType(0x03, IngestEventReceptor.class);
	
	/**
	 * 
	 */
	public static SynapticMessageType[] types = new SynapticMessageType[] {RELAY_EVENT};
	
	/**
	 * Returns the MessageType instance by the type supplied
	 * 
	 * @param type
	 * @throws InvalidMessageTypeException
	 * @return MessageType
	 */
	public static SynapticMessageType getMessageType(byte type) throws InvalidMessageTypeException
	{
		for (SynapticMessageType messageType : types) 
			if(messageType.getType() == ByteUtil.convertToInt(type))
				return messageType;
		throw new InvalidMessageTypeException(new Exception("Invalid message type"));
	}
}
