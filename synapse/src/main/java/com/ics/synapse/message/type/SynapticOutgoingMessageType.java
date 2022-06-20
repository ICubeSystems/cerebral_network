package com.ics.synapse.message.type;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.OutgoingMessageType;
import com.ics.synapse.affector.CredentialsAffector;
import com.ics.synapse.affector.DeletePorAffector;
import com.ics.synapse.affector.PublishedEventAffector;
import com.ics.synapse.affector.ReadyConfirmedAffector;
import com.ics.synapse.affector.RelayEventAcknowledgementAffector;
import com.ics.synapse.affector.StartupAffector;
import com.ics.synapse.affector.ThreeWayEventAcknowledgementAffector;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public class SynapticOutgoingMessageType extends OutgoingMessageType 
{

	public SynapticOutgoingMessageType(int type, Class<? extends Affector> affectorClass, String typeName) 
	{
		super(type, affectorClass, typeName);
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static SynapticOutgoingMessageType PUBLISH_EVENT = new SynapticOutgoingMessageType(0x03, PublishedEventAffector.class, "PUBLISH_EVENT");
	
	/**
	 * This message type is used to startup the connection
	 */
	public static SynapticOutgoingMessageType ACK_RECEIVED = new SynapticOutgoingMessageType(0x05, ThreeWayEventAcknowledgementAffector.class, "ACK_RECEIVED");

	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static SynapticOutgoingMessageType RELAYED_EVENT_ACK = new SynapticOutgoingMessageType(0x04, RelayEventAcknowledgementAffector.class, "RELAYED_EVENT_ACK");
	
	/**
	 * Synaptic node sends a notification that relay event acknowledged successfully and POR is deleted from snaptic side.
	 */
	public static SynapticOutgoingMessageType POR_DELETED = new SynapticOutgoingMessageType(0x0D, DeletePorAffector.class, "POR_DELETED");
	
	/**
	 * 
	 */
	public static SynapticOutgoingMessageType STARTUP = new SynapticOutgoingMessageType(0x00, StartupAffector.class, "STARTUP");
	/**
	 *  Credentials
	 */
	public static SynapticOutgoingMessageType CREDENTIALS = new SynapticOutgoingMessageType(0x01, CredentialsAffector.class, "CREDENTIALS");
	/**
	 * 
	 */
	public static SynapticOutgoingMessageType READY_CONFIRM = new SynapticOutgoingMessageType(0x0E, ReadyConfirmedAffector.class, "READY_CONFIRM");
	/**
	 * 
	 */ 
	public static SynapticOutgoingMessageType[] types = new SynapticOutgoingMessageType[] {PUBLISH_EVENT, STARTUP, CREDENTIALS, READY_CONFIRM, ACK_RECEIVED, RELAYED_EVENT_ACK, POR_DELETED};


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
		throw new InvalidMessageTypeException(new Exception("Invalid message type"+type));
	}

}
