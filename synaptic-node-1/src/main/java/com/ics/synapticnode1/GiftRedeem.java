package com.ics.synapticnode1;

import java.io.IOException;
import java.io.Serializable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.synapse.ncephEvent.NcephEvent;
/**
 * 
 * @author Chandan Verma
 * @since 04-Mar-2022
 */

public class GiftRedeem implements Serializable, NcephEvent
{
	private static final long serialVersionUID = -3996054630188026396L;

	private String giftCode;

	private String recipientName;

	private String senderName;

	private String redeemOn;
	
	public GiftRedeem(String giftCode, String recipientName, String senderName, String redeemOn)
	{
		this.giftCode = giftCode;
		this.recipientName = recipientName;
		this.senderName = senderName;
		this.redeemOn = redeemOn;
	}
	
	public String getGiftCode()
	{
		return giftCode;
	}

	public String getRecipientName()
	{
		return recipientName;
	}

	public String getSenderName()
	{
		return senderName;
	}
	
	public String getDeliveredOn()
	{
		return redeemOn;
	}

	public String toJSON() throws JsonProcessingException
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
	
	@Override
	public EventData toEvent() throws JsonProcessingException, IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException
	{
		return new EventData.Builder().eventId(2000).objectJSON(toJSON()).build();
	}

	public static class Builder
	{
		private String giftCode;

		private String recipientName;

		private String senderName;

		private String redeemOn;
		
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
		
		public Builder redeemOn(String redeemOn)
		{
			this.redeemOn = redeemOn;
			return this;
		}
		
		public GiftRedeem build()
		{
			return new GiftRedeem(giftCode, recipientName, senderName, redeemOn);
		}
		
	}
	
}
