package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.event.ThreeWayAcknowledgement;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anshul
 * @since 29-Mar-2022
 */
public abstract class ThreeWayAcknowledgementReceptor extends Receptor 
{
	private ThreeWayAcknowledgement threeWayAck;
	
	public ThreeWayAcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			threeWayAck = (ThreeWayAcknowledgement) message.decoder().getData(ThreeWayAcknowledgement.class);
		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
	}

	public ThreeWayAcknowledgement getThreeWayAcknowledgement() {
		return threeWayAck;
	}
}
