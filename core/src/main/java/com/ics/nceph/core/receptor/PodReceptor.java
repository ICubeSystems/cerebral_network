package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
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
			// LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.description("Class Name: " + this.getClass().getSimpleName())
					.action("AcknowledgementDone data mapping failed")
					.logError(),e);
		}
	}

	public AcknowledgementDoneData getPod() {
		return pod;
	}
}
