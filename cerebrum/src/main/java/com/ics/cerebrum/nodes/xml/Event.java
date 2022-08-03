package com.ics.cerebrum.nodes.xml;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 28-Jul-2022
 */
@XmlRootElement(name = "event")
public class Event 
{
	private Integer eventType;
	
	private String eventReceptor;

	public Integer getEventType() {
		return eventType;
	}

	public void setEventType(Integer eventType) {
		this.eventType = eventType;
	}

	public String getEventReceptor() {
		return eventReceptor;
	}

	public void setEventReceptor(String eventReceptor) {
		this.eventReceptor = eventReceptor;
	}
}