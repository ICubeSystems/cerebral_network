package com.ics.cerebrum.nodes.xml;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 15-Feb-2022
 */
@XmlRootElement(name = "subscriptions")
public class Subscriptions 
{
	private List<Event> event;

	public List<Event> getEvent() {
		return event;
	}

	public void setEvent(List<Event> event) {
		this.event = event;
	}

	
}
