package com.ics.cerebrum.message.type;

import com.ics.cerebrum.receptor.BootstrapReceptor;
import com.ics.cerebrum.receptor.CredentialsReceptor;
import com.ics.cerebrum.receptor.PorDeletedReceptor;
import com.ics.cerebrum.receptor.PublishedEventReceptor;
import com.ics.cerebrum.receptor.PublishedEventThreeWayAcknowledgementReceptor;
import com.ics.cerebrum.receptor.ReadyConfirmedReceptor;
import com.ics.cerebrum.receptor.RelayedEventAcknowledgeReceptor;
import com.ics.cerebrum.receptor.StartupReceptor;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.message.type.IncomingMessageType;
import com.ics.nceph.core.message.type.MessageClassification;
import com.ics.nceph.core.receptor.PauseTransmissionReceptor;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.nceph.core.receptor.ResumeTransmissionReceptor;
import com.ics.util.ByteUtil;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 06-Jan-2022
 */
public class CerebralIncomingMessageType extends IncomingMessageType
{
	CerebralIncomingMessageType(int type, Class<? extends Receptor> processorClass, String typeName, MessageClassification classification) 
	{
		super(type, processorClass, typeName, classification);
	}
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static CerebralIncomingMessageType PUBLISH_EVENT = new CerebralIncomingMessageType(3, PublishedEventReceptor.class, "PUBLISH_EVENT", MessageClassification.PUBLISH);
	/**
	 * This message type is used to startup the connection
	 */
	public static CerebralIncomingMessageType STARTUP = new CerebralIncomingMessageType(0, StartupReceptor.class, "STARTUP", MessageClassification.AUTHENICATION);
	/**
	 * This message type is used to Credentials the connection
	 */
	public static CerebralIncomingMessageType CREDENTIALS = new CerebralIncomingMessageType(1, CredentialsReceptor.class, "CREDENTIALS", MessageClassification.AUTHENICATION);
	/**
	 * This message type is used to Ready_Confirm the connection
	 */
	public static CerebralIncomingMessageType READY_CONFIRMED = new CerebralIncomingMessageType(14, ReadyConfirmedReceptor.class, "READY_CONFIRMED", MessageClassification.AUTHENICATION);
	
	/**
	 * This message type is used to publish an event in the network
	 */
	public static CerebralIncomingMessageType ACK_RECEIVED = new CerebralIncomingMessageType(5, PublishedEventThreeWayAcknowledgementReceptor.class, "ACK_RECEIVED", MessageClassification.PUBLISH);
	/**
	 * This message type is used to receive acknowledgement of publish event
	 */
	public static CerebralIncomingMessageType RELAYED_EVENT_ACK = new CerebralIncomingMessageType(4, RelayedEventAcknowledgeReceptor.class, "RELAYED_EVENT_ACK", MessageClassification.RELAY);
	
	/**
	 * Synaptic node sends a notification that relay event acknowledged successfully and POR is deleted from synaptic side.
	 */
	public static CerebralIncomingMessageType POR_DELETED = new CerebralIncomingMessageType(13, PorDeletedReceptor.class, "POR_DELETED", MessageClassification.RELAY);
	/**
	 * 
	 */
	public static CerebralIncomingMessageType BOOTSTRAP = new CerebralIncomingMessageType(15, BootstrapReceptor.class, "BOOTSTRAP",MessageClassification.CONTROL);
	
	/**
	 * This message type is used to tell Cerebrum to pause sending messages
	 */
	public static CerebralIncomingMessageType PAUSE_TRANSMISSION = new CerebralIncomingMessageType(17, PauseTransmissionReceptor.class, "PAUSETRANSMISSION", MessageClassification.BACKPRESSURE);
	
	/**
	 * This message type is used to tell Cerebrum to resume sending messages
	 */
	public static CerebralIncomingMessageType RESUME_TRANSMISSION = new CerebralIncomingMessageType(18, ResumeTransmissionReceptor.class, "RESUMETRANSMISSION", MessageClassification.BACKPRESSURE);
	
	
	/**
	 * 
	 */
	public static CerebralIncomingMessageType[] types = new CerebralIncomingMessageType[] {
															BOOTSTRAP, 
															PUBLISH_EVENT, 
															STARTUP, 
															CREDENTIALS, 
															READY_CONFIRMED, 
															PUBLISH_EVENT,
															ACK_RECEIVED,
															RELAYED_EVENT_ACK,
															POR_DELETED,
															PAUSE_TRANSMISSION,
															RESUME_TRANSMISSION};

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
