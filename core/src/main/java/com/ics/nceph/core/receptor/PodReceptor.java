package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.AcknowledgementDoneData;

/**
 * 
 * @author Anshul
 * @since 29-Mar-2022
 */
public abstract class PodReceptor extends Receptor 
{
	private AcknowledgementDoneData pod;
	
	public PodReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			pod = (AcknowledgementDoneData) message.decoder().getData(AcknowledgementDoneData.class);
		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
	}

	public AcknowledgementDoneData getPod() {
		return pod;
	}
}
