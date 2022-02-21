package com.ics.nceph.core.message;

import java.util.Date;

import com.ics.nceph.core.event.Event;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public class ProofOfDelivery 
{
	private String messageId;
	
	private Event event;
	
	private Date publishedOn;
	
	private Date ackReceivedOn;
	
	ProofOfDelivery(){}
	
	ProofOfDelivery(String messageId, Event event, Date publishedOn)
	{
		this.publishedOn = publishedOn;
		this.messageId = messageId;
		this.event = event;
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public Event getEvent() {
		return event;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}

	public Date getPublishedOn() {
		return publishedOn;
	}
	
	public void setPublishedOn(Date publishedOn) {
		this.publishedOn = publishedOn;
	}

	public Date getAckReceivedOn() {
		return ackReceivedOn;
	}

	public void setAckReceivedOn(Date ackReceivedOn) 
	{
		this.ackReceivedOn = ackReceivedOn;
	}
	
	public static class Builder
	{
		private String messageId;
		
		private Event event;
		
		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
			return this;
		}
		
		public Builder event(Event event)
		{
			this.event = event;
			return this;
		}
		
		public ProofOfDelivery build()
		{
			return new ProofOfDelivery(messageId, event, new Date());
		}
	}
}
