package com.ics.menu;

import com.ics.synapse.ncephEvent.Event;
/**
 * 
 * @author Chandan Verma
 * @since 04-Mar-2022
 */

public class GiftRedeem extends Event
{
	private static final long serialVersionUID = -3996054630188026396L;
	
	private String giftCode;

	private String recipientName;

	private String senderName;

	private String redeemOn;
	
	public GiftRedeem(String giftCode, String recipientName, String senderName, String redeemOn)
	{
		setType(2);
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
