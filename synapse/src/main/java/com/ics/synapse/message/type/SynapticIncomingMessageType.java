package com.ics.synapse.message.type;

import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.IncomingMessageType;
import com.ics.nceph.core.message.type.MessageClassification;
import com.ics.nceph.core.receptor.PauseTransmissionReceptor;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.nceph.core.receptor.ResumeTransmissionReceptor;
import com.ics.synapse.receptor.AuthErrorReceptor;
import com.ics.synapse.receptor.AuthenticationReceptor;
import com.ics.synapse.receptor.ConfigReceptor;
import com.ics.synapse.receptor.DeletePodReceptor;
import com.ics.synapse.receptor.PublishedEventAcknowledgementReceptor;
import com.ics.synapse.receptor.ReadyReceptor;
import com.ics.synapse.receptor.RelayEventThreeWayAcknowledgementReceptor;
import com.ics.synapse.receptor.RelayedEventReceptor;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 06-Jan-2022
 */
public class SynapticIncomingMessageType extends IncomingMessageType
{

	public SynapticIncomingMessageType(int type, Class<? extends Receptor> processorClass, String typeName, MessageClassification classification) 
	{
		super(type, processorClass, typeName, classification);
	}
	
	/**
	 * This message type is used to receive event in the network
	 */
	public static SynapticIncomingMessageType RELAY_EVENT = new SynapticIncomingMessageType(11, RelayedEventReceptor.class, "RELAY_EVENT", MessageClassification.RELAY);
	
	/**
	 * This message type is used to Authenticate the network
	 */

	public static SynapticIncomingMessageType AUTHENTICATE = new SynapticIncomingMessageType(6, AuthenticationReceptor.class, "AUTHENTICATE", MessageClassification.AUTHENICATION);
	/**
	 * This message type is used to Ready the network
	 */
	public static SynapticIncomingMessageType READY = new SynapticIncomingMessageType(7, ReadyReceptor.class, "READY", MessageClassification.AUTHENICATION);
	/**
	 * This message type is used to Error (AUTH_FAILED) the network
	 */
	public static SynapticIncomingMessageType ERROR = new SynapticIncomingMessageType(8, AuthErrorReceptor.class, "ERROR", MessageClassification.AUTHENICATION);
	/**
	 * This message type is used to receive acknowledgement of publish event
	 */
	public static SynapticIncomingMessageType NCEPH_EVENT_ACK = new SynapticIncomingMessageType(9, PublishedEventAcknowledgementReceptor.class, "NCEPH_EVENT_ACK", MessageClassification.PUBLISH);
	
	/**
	 * This message type is used to startup the connection
	 */
	public static SynapticIncomingMessageType RELAY_ACK_RECEIVED = new SynapticIncomingMessageType(12, RelayEventThreeWayAcknowledgementReceptor.class, "RELAY_ACK_RECEIVED", MessageClassification.RELAY);
	
	/**
	 * This message type is used to receive acknowledgement of publish event
	 */
	public static SynapticIncomingMessageType DELETE_POD = new SynapticIncomingMessageType(10, DeletePodReceptor.class, "DELETE_POD", MessageClassification.PUBLISH);
	/**
	 * This message type is used to receive acknowledgement of publish event
	 */
	public static SynapticIncomingMessageType CONFIG = new SynapticIncomingMessageType(16, ConfigReceptor.class, "CONFIG", MessageClassification.CONTROL);
	
	/**
	 * This message type is used to tell Synapse to pause sending messages
	 */
	public static SynapticIncomingMessageType PAUSE_TRANSMISSION = new SynapticIncomingMessageType(17, PauseTransmissionReceptor.class, "PAUSETRANSMISSION", MessageClassification.BACKPRESSURE);
	
	/**
	 * This message type is used to tell Synapse to resume sending messages
	 */
	public static SynapticIncomingMessageType RESUME_TRANSMISSION = new SynapticIncomingMessageType(18, ResumeTransmissionReceptor.class, "RESUMETRANSMISSION", MessageClassification.BACKPRESSURE);
	
	
	/**
	 * 
	 */
	public static SynapticIncomingMessageType[] types = new SynapticIncomingMessageType[] {
															CONFIG,
															NCEPH_EVENT_ACK,
															DELETE_POD,
															RELAY_ACK_RECEIVED,
															RELAY_EVENT, 
															AUTHENTICATE, 
															READY, ERROR, 
															PAUSE_TRANSMISSION, 
															RESUME_TRANSMISSION};
	
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
