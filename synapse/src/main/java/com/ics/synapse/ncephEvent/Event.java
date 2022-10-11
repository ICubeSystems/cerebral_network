package com.ics.synapse.ncephEvent;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.event.EventData;

public class Event implements NcephEvent, Serializable
{
	private static final long serialVersionUID = -3996054630188029806L;
	
	Integer type;
	
	public void setType(Integer type) {
		this.type = type;
	}
	
	@Override
	public EventData toEvent(Integer eventId) throws IOException
	{
		EventData eventData = new EventData.Builder()
				.createdOn(new Date().getTime())
				.eventId(eventId)
				.eventType(type)
				.objectJSON(toJSON())
				.build();
		return eventData;
	}
	
	public String toJSON() throws JsonProcessingException 
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
}
