package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementData;

/**
 * 
 * @author Anshul
 * @since 29-Mar-2022
 */
public abstract class AcknowledgementReceptor extends Receptor 
{
	private AcknowledgementData ack;
	
	public AcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			ack = (AcknowledgementData) message.decoder().getData(AcknowledgementData.class);
		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
	}

	public AcknowledgementData getAcknowledgement() {
		return ack;
	}
}
