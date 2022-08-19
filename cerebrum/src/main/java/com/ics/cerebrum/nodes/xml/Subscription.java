package com.ics.cerebrum.nodes.xml;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 28-Jul-2022
 */
@XmlRootElement(name = "subscription")
public class Subscription 
{
	private Integer eventType;
	
	private String applicationReceptor;

	public Integer getEventType() {
		return eventType;
	}

	public void setEventType(Integer eventType) {
		this.eventType = eventType;
	}

	public String getApplicationReceptor() {
		return applicationReceptor;
	}

	public void setApplicationReceptor(String eventReceptor) {
		this.applicationReceptor = eventReceptor;
	}
}