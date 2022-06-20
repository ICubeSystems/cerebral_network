package com.ics.synapse.message.type;

import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.IncomingMessageType;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.receptor.AuthErrorReceptor;
import com.ics.synapse.receptor.AuthenticationReceptor;
import com.ics.synapse.receptor.DeletePodReceptor;
import com.ics.synapse.receptor.EventAcknowledgementReceptor;
import com.ics.synapse.receptor.ReadyReceptor;
import com.ics.synapse.receptor.RelayedEventReceptor;
import com.ics.synapse.receptor.ThreeWayRelayEventAcknowledgementReceptor;
import com.ics.util.ByteUtil;

public class SynapticIncomingMessageType extends IncomingMessageType
{

	public SynapticIncomingMessageType(int type, Class<? extends Receptor> processorClass, String typeName) 
	{
		super(type, processorClass, typeName);
	}
	
	/**
	 * This message type is used to recieve event in the network
	 */
	public static SynapticIncomingMessageType RELAY_EVENT = new SynapticIncomingMessageType(0x0B, RelayedEventReceptor.class, "RELAY_EVENT");
	
	/**
	 * This message type is used to Authenticate the network
	 */
	public static SynapticIncomingMessageType AUTHENTICATE = new SynapticIncomingMessageType(0x06, AuthenticationReceptor.class, "AUTHENTICATE");
	/**
	 * This message type is used to Ready the network
	 */
	public static SynapticIncomingMessageType READY = new SynapticIncomingMessageType(0x07, ReadyReceptor.class, "READY");
	/**
	 * This message type is used to Error (AUTH_FAILED) the network
	 */
	public static SynapticIncomingMessageType ERROR = new SynapticIncomingMessageType(0x08, AuthErrorReceptor.class, "ERROR");
	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static SynapticIncomingMessageType NCEPH_EVENT_ACK = new SynapticIncomingMessageType(0x09, EventAcknowledgementReceptor.class, "NCEPH_EVENT_ACK");
	
	/**
	 * This message type is used to startup the connection
	 */
	public static SynapticIncomingMessageType RELAY_ACK_RECEIVED = new SynapticIncomingMessageType(0x0C, ThreeWayRelayEventAcknowledgementReceptor.class, "RELAY_ACK_RECEIVED");
	
	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static SynapticIncomingMessageType DELETE_POD = new SynapticIncomingMessageType(0x0A, DeletePodReceptor.class, "DELETE_POD");
	/**
	 * 
	 */
	public static SynapticIncomingMessageType[] types = new SynapticIncomingMessageType[] {RELAY_EVENT,NCEPH_EVENT_ACK,DELETE_POD,RELAY_ACK_RECEIVED, RELAY_EVENT, AUTHENTICATE, READY, ERROR};
	
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
