package com.ics.cerebrum.message.type;

import com.ics.cerebrum.receptor.PorDeletedReceptor;
import com.ics.cerebrum.receptor.StartupReceptor;
import com.ics.cerebrum.receptor.CredentialsReceptor;
import com.ics.cerebrum.receptor.PublishedEventReceptor;
import com.ics.cerebrum.receptor.RelayedEventAcknowledgeReceptor;
import com.ics.cerebrum.receptor.ThreeWayEventAcknowledgementReceptor;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.IncomingMessageType;
import com.ics.cerebrum.receptor.ReadyConfirmedReceptor;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 06-Jan-2022
 */
public class CerebralIncomingMessageType extends IncomingMessageType
{
	CerebralIncomingMessageType(int type, Class<? extends Receptor> processorClass, String typeName) 
	{
		super(type, processorClass, typeName);
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static CerebralIncomingMessageType PUBLISH_EVENT = new CerebralIncomingMessageType(0x03, PublishedEventReceptor.class, "PUBLISH_EVENT");
	/**
	 * This message type is used to startup the connection
	 */
	public static CerebralIncomingMessageType STARTUP = new CerebralIncomingMessageType(0x00, StartupReceptor.class, "STARTUP");
	/**
	 * This message type is used to Credentials the connection
	 */
	public static CerebralIncomingMessageType CREDENTIALS = new CerebralIncomingMessageType(0x01, CredentialsReceptor.class, "CREDENTIALS");
	/**
	 * This message type is used to Ready_Confirm the connection
	 */
	public static CerebralIncomingMessageType READY_CONFIRMED = new CerebralIncomingMessageType(0x0E, ReadyConfirmedReceptor.class, "READY_CONFIRMED");
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static CerebralIncomingMessageType ACK_RECEIVED = new CerebralIncomingMessageType(0x05, ThreeWayEventAcknowledgementReceptor.class, "ACK_RECEIVED");
	/**
	 * This message type is used to recieve acknowledgement of publish event
	 */
	public static CerebralIncomingMessageType RELAYED_EVENT_ACK = new CerebralIncomingMessageType(0x04, RelayedEventAcknowledgeReceptor.class, "RELAYED_EVENT_ACK");
	
	/**
	 * Synaptic node sends a notification that relay event acknowledged successfully and POR is deleted from snaptic side.
	 */
	public static CerebralIncomingMessageType POR_DELETED = new CerebralIncomingMessageType(0x0D, PorDeletedReceptor.class, "POR_DELETED");
	
	/**
	 * 
	 */
	public static CerebralIncomingMessageType[] types = new CerebralIncomingMessageType[] {PUBLISH_EVENT, STARTUP, CREDENTIALS, READY_CONFIRMED, PUBLISH_EVENT,ACK_RECEIVED,RELAYED_EVENT_ACK,POR_DELETED};

	/**
	 * Returns the MessageType instance by the type supplied
	 * 
	 * @param type
	 * @throws InvalidMessageTypeException
	 * @return MessageType
	 */
	public static CerebralIncomingMessageType getMessageType(byte type) throws InvalidMessageTypeException
	{
		for (CerebralIncomingMessageType messageType : types) 
			if(messageType.getType() == ByteUtil.convertToInt(type))
				return messageType;
		throw new InvalidMessageTypeException(new Exception("Invalid message type"));
	}
}
