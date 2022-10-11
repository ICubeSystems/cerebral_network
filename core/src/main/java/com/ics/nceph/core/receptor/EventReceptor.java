package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 26-Jan-2022
 */
public abstract class EventReceptor extends Receptor 
{
	private EventData event;

	public EventReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			event = (EventData) message.decoder().getData(EventData.class);
		} catch (JsonProcessingException e) 
		{
			// LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.description("Class Name: " + this.getClass().getSimpleName())
					.action("Event data mapping failed")
					.logError(),e);
		}
	}

	public EventData getEvent() {
		return event;
	}
}
