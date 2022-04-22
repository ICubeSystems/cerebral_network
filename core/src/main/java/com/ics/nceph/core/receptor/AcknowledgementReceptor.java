package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.event.Acknowledgement;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anshul
 * @since 29-Mar-2022
 */
public abstract class AcknowledgementReceptor extends Receptor 
{
	private Acknowledgement ack;
	
	public AcknowledgementReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			ack = (Acknowledgement) message.decoder().getData(Acknowledgement.class);
		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
	}

	public Acknowledgement getAcknowledgement() {
		return ack;
	}
}
