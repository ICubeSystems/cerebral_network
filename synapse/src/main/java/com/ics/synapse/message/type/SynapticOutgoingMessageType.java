package com.ics.synapse.message.type;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.OutgoingMessageType;
import com.ics.synapse.affector.PublishedEventAffector;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public class SynapticOutgoingMessageType extends OutgoingMessageType 
{

	public SynapticOutgoingMessageType(int type, Class<? extends Affector> affectorClass) 
	{
		super(type, affectorClass);
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static SynapticOutgoingMessageType PUBLISH_EVENT = new SynapticOutgoingMessageType(0x03, PublishedEventAffector.class);
	
	/**
	 * This message type is used to startup the connection
	 */
	//public static SynapticOutgoingMessageType STARTUP = new SynapticOutgoingMessageType(0x00, PublishedEventAffector.class);
	
	/**
	 * 
	 */
	public static SynapticOutgoingMessageType[] types = new SynapticOutgoingMessageType[] {PUBLISH_EVENT};
	
	/**
	 * Returns the MessageType instance by the type supplied
	 * 
	 * @param type
	 * @throws InvalidMessageTypeException
	 * @return MessageType
	 */
	public static SynapticOutgoingMessageType getMessageType(byte type) throws InvalidMessageTypeException
	{
		for (SynapticOutgoingMessageType messageType : types) 
			if(messageType.getType() == ByteUtil.convertToInt(type))
				return messageType;
		throw new InvalidMessageTypeException(new Exception("Invalid message type"));
	}

}
