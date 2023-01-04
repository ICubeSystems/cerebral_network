package com.ics.cerebrum.message.type;

import com.ics.cerebrum.affector.AuthErrorAffector;
import com.ics.cerebrum.affector.AuthenticationAffector;
import com.ics.cerebrum.affector.ConfigAffector;
import com.ics.cerebrum.affector.DeletePodAffector;
import com.ics.cerebrum.affector.EventAcknowledgementAffector;
import com.ics.cerebrum.affector.ReadyAffector;
import com.ics.cerebrum.affector.RelayEventThreeWayAcknowledgementAffector;
import com.ics.cerebrum.affector.RelayedEventAffector;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.affector.PauseTransmissionAffector;
import com.ics.nceph.core.affector.ResumeTransmissionAffector;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.OutgoingMessageType;
import com.ics.util.ByteUtil;

public class CerebralOutgoingMessageType extends OutgoingMessageType 
{
	public CerebralOutgoingMessageType(int type, Class<? extends Affector> affectorClass, String typeName) 
	{
		super(type, affectorClass, typeName);
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static CerebralOutgoingMessageType RELAY_EVENT = new CerebralOutgoingMessageType(11, RelayedEventAffector.class, "RELAY_EVENT");
	
	/**
	 * This message type is used to receive acknowledgement of publish event
	 */
	public static CerebralOutgoingMessageType NCEPH_EVENT_ACK = new CerebralOutgoingMessageType(9, EventAcknowledgementAffector.class, "NCEPH_EVENT_ACK");
	
	/**
	 * This message type is used to receive acknowledgement of publish event
	 */
	public static CerebralOutgoingMessageType DELETE_POD = new CerebralOutgoingMessageType(10, DeletePodAffector.class, "DELETE_POD");
	
	/**
	 * This message type is used to startup the connection
	 */
	public static CerebralOutgoingMessageType RELAY_ACK_RECEIVED = new CerebralOutgoingMessageType(12, RelayEventThreeWayAcknowledgementAffector.class, "RELAY_ACK_RECEIVED");
	
	/**
	 * This message type is used to Authentication connection in the network
	 */
	public static CerebralOutgoingMessageType AUTHENTICATE = new CerebralOutgoingMessageType(6, AuthenticationAffector.class, "AUTHENTICATE");
	
	/**
	 * This message type is used to set READY the connection state
	 */
	public static CerebralOutgoingMessageType READY = new CerebralOutgoingMessageType(7, ReadyAffector.class, "READY");
	
	/**
	 * This message type is used to set AUTH_FAILED the connection state
	 */
	public static CerebralOutgoingMessageType ERROR = new CerebralOutgoingMessageType(8, AuthErrorAffector.class, "ERROR");
	
	/**
	 * This message type is used to set AUTH_FAILED the connection state
	 */
	public static CerebralOutgoingMessageType CONFIG = new CerebralOutgoingMessageType(16, ConfigAffector.class, "CONFIG");
	
	/**
	 * This message type is used to tell Synapse to pause sending messages
	 */
	public static CerebralOutgoingMessageType PAUSE_TRANSMISSION = new CerebralOutgoingMessageType(17, PauseTransmissionAffector.class, "PAUSETRANSMISSION");
	
	/**
	 * This message type is used to tell Synapse to resume sending messages
	 */
	public static CerebralOutgoingMessageType RESUME_TRANSMISSION = new CerebralOutgoingMessageType(18, ResumeTransmissionAffector.class, "RESUMETRANSMISSION");
	
	/**
	 * 
	 */
	public static CerebralOutgoingMessageType[] types = new CerebralOutgoingMessageType[] {
															CONFIG,
															RELAY_EVENT,
															AUTHENTICATE,
															READY, ERROR,
															RELAY_EVENT,
															NCEPH_EVENT_ACK,
															DELETE_POD,
															RELAY_ACK_RECEIVED,
															PAUSE_TRANSMISSION,
															RESUME_TRANSMISSION
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
