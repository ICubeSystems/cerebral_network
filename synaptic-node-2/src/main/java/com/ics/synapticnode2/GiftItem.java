package com.ics.synapticnode2;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.event.Event;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;

public class GiftItem implements Serializable
{
	private static final long serialVersionUID = -3996054630188026396L;

	private String giftCode;
	
	private String recipientName;
	
	private String senderName;
	
	private BigDecimal amount;
	
	private BigDecimal balance;
	
	public GiftItem(String giftCode, String recipientName, String senderName, BigDecimal amount, BigDecimal balance) 
	{
		this.giftCode = giftCode;
		this.recipientName = recipientName;
		this.senderName = senderName;
		this.amount = amount;
		this.balance = balance;
	}
	
	public String getGiftCode() {
		return giftCode;
	}

	public String getRecipientName() {
		return recipientName;
	}

	public String getSenderName() {
		return senderName;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public BigDecimal getBalance() {
		return balance;
	}
	
	public String toJSON() throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
	
	public Event toEvent() throws JsonProcessingException, IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException
	{
		return new Event.Builder().eventId(1000).objectJSON(toJSON()).build();
	}
	
	public static class Builder
	{
		private String giftCode;
		
		private String recipientName;
		
		private String senderName;
		
		BigDecimal amount;
		
		BigDecimal balance;
		
		public Builder giftCode(String giftCode)
		{
			this.giftCode = giftCode;
			return this;
		}
		
		public Builder recipientName(String recipientName)
		{
			this.recipientName = recipientName;
			return this;
		}
		
		public Builder senderName(String senderName)
		{
			this.senderName = senderName;
			return this;
		}
		
		public Builder amount(BigDecimal amount)
		{
			this.amount = amount;
			return this;
		}
		
		public Builder balance(BigDecimal balance)
		{
			this.balance = balance;
			return this;
		}
		
		public GiftItem build()
		{
			return new GiftItem(giftCode, recipientName, senderName, amount, balance);
		}
	}
}
