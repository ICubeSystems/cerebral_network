package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ThreeWayAcknowledgementData;

/**
 * 
 * @author Anshul
 * @since 29-Mar-2022
 */
public abstract class ThreeWayAcknowledgementReceptor extends Receptor 
{
	private ThreeWayAcknowledgementData threeWayAck;
	
	public ThreeWayAcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			threeWayAck = (ThreeWayAcknowledgementData) message.decoder().getData(ThreeWayAcknowledgementData.class);
		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
	}

	public ThreeWayAcknowledgementData getThreeWayAcknowledgement() {
		return threeWayAck;
	}
}
