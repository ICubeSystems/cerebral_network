package com.ics.synapse.message.type;

import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.IncomingMessageType;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.receptor.DeletePodReceptor;
import com.ics.synapse.receptor.EventAcknowledgementReceptor;
import com.ics.synapse.receptor.RelayedEventReceptor;
import com.ics.synapse.receptor.ThreeWayRelayEventAcknowledgementReceptor;
import com.ics.util.ByteUtil;

public class SynapticIncomingMessageType extends IncomingMessageType
{

	public SynapticIncomingMessageType(int type, Class<? extends Receptor> processorClass) 
	{
		super(type, processorClass);
	}
	
	/**
	 * This message type is used to recieve event in the network
	 */
	public static SynapticIncomingMessageType RELAY_EVENT = new SynapticIncomingMessageType(0x0B, RelayedEventReceptor.class);
	
	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static SynapticIncomingMessageType NCEPH_EVENT_ACK = new SynapticIncomingMessageType(0x09, EventAcknowledgementReceptor.class);
	
	/**
	 * This message type is used to startup the connection
	 */
	public static SynapticIncomingMessageType RELAY_ACK_RECEIVED = new SynapticIncomingMessageType(0x0C, ThreeWayRelayEventAcknowledgementReceptor.class);
	
	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static SynapticIncomingMessageType DELETE_POD = new SynapticIncomingMessageType(0x0A, DeletePodReceptor.class);
	/**
	 * 
	 */
	public static SynapticIncomingMessageType[] types = new SynapticIncomingMessageType[] {RELAY_EVENT,NCEPH_EVENT_ACK,DELETE_POD,RELAY_ACK_RECEIVED};
	
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
