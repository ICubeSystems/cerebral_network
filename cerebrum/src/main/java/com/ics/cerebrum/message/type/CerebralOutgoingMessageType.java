package com.ics.cerebrum.message.type;

import com.ics.cerebrum.affector.DeletePodAffector;
import com.ics.cerebrum.affector.EventAcknowledgementAffector;
import com.ics.cerebrum.affector.AuthErrorAffector;
import com.ics.cerebrum.affector.AuthenticationAffector;
import com.ics.cerebrum.affector.ReadyAffector;
import com.ics.cerebrum.affector.RelayedEventAffector;
import com.ics.cerebrum.affector.ThreeWayRelayEventAcknowledgementAffector;
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
	public static CerebralOutgoingMessageType RELAY_EVENT = new CerebralOutgoingMessageType(0x0B, RelayedEventAffector.class);
	
	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static CerebralOutgoingMessageType NCEPH_EVENT_ACK = new CerebralOutgoingMessageType(0x09, EventAcknowledgementAffector.class);
	
	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static CerebralOutgoingMessageType DELETE_POD = new CerebralOutgoingMessageType(0x0A, DeletePodAffector.class);
	
	/**
	 * This message type is used to startup the connection
	 */
	public static CerebralOutgoingMessageType RELAY_ACK_RECEIVED = new CerebralOutgoingMessageType(0x0C, ThreeWayRelayEventAcknowledgementAffector.class);
	
	/**
	 * This message type is used to Authentication Message in the network
	 */
	public static CerebralOutgoingMessageType AUTHENTICATE = new CerebralOutgoingMessageType(0x06, AuthenticationAffector.class);
	/**
	 * This message type is used to set READY the connection state
	 */
	public static CerebralOutgoingMessageType READY = new CerebralOutgoingMessageType(0x07, ReadyAffector.class);
	/**
	 * This message type is used to set AUTH_FAILED the connection state
	 */
	public static CerebralOutgoingMessageType ERROR = new CerebralOutgoingMessageType(0x08, AuthErrorAffector.class);
	/**
	 * 
	 */
	public static CerebralOutgoingMessageType[] types = new CerebralOutgoingMessageType[] {
															RELAY_EVENT,
															AUTHENTICATE,
															READY, ERROR,
															RELAY_EVENT,
															NCEPH_EVENT_ACK,
															DELETE_POD,
															RELAY_ACK_RECEIVED
															};  
	
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
