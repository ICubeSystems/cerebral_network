package com.ics.nceph.core.receptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.event.Event;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 26-Jan-2022
 */
public abstract class EventReceptor extends Receptor 
{
	private Event event;
	
	public EventReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try 
		{
			event = (Event) message.decoder().getData(Event.class);
		} catch (JsonProcessingException e) 
		{
			e.printStackTrace();
		}
	}
	
	public Event getEvent() {
		return event;
	}
}
