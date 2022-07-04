package com.ics.menu;

import com.ics.synapse.ncephEvent.Event;
/**
 * 
 * @author Chandan Verma
 * @since 04-Mar-2022
 */

public class GiftStatus extends Event
{
	private static final long serialVersionUID = 1L;
	
	private String giftCode;
	
	private String recipientName;
	
	private String senderName;
	
	private String sendOnEmail;
	
	private String sendOnMobile;

	public GiftStatus(String giftCode,String recipientName,String senderName,String sendOnEmail,String sendOnMobile) {
		setType(5);
		this.giftCode = giftCode;
		this.recipientName = recipientName;
		this.senderName = senderName;
		this.sendOnEmail = sendOnEmail;
		this.sendOnMobile = sendOnMobile;
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
	
	public String getSendOnEmail()
	{
		return sendOnEmail;
	}

	public String getSendOnMobile()
	{
		return sendOnMobile;
	}

	public static class Builder
	{
		private String giftCode;
		
		private String recipientName;
		
		private String senderName;
		
		private String sendOnEmail;
		
		private String sendOnMobile;
		
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
		
		public Builder sendOnEmail(String sendOnEmail)
		{
			this.sendOnEmail = sendOnEmail;
			return this;
		}
		
		public Builder sendOnMobile(String sendOnMobile)
		{
			this.sendOnMobile = sendOnMobile;
			return this;
		}
		
		public GiftStatus build()
		{
			return new GiftStatus(giftCode,recipientName,senderName,sendOnEmail,sendOnMobile);
		}
		
	}

}
