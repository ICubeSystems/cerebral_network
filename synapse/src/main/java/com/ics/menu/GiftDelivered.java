package com.ics.menu;

import java.math.BigDecimal;

import com.ics.synapse.ncephEvent.Event;

/**
 * 
 * @author Chandan Verma
 * @since 04-Mar-2022
 */

public class GiftDelivered extends Event
{
	private static final long serialVersionUID = -3996054630188026396L;
	
	private String giftCode;

	private String recipientName;

	private String senderName;

	private BigDecimal amount;

	private BigDecimal balance;

	private String deliveredOn;

	public GiftDelivered(String giftCode, String recipientName,String senderName, BigDecimal amount, BigDecimal balance,String deliveredOn)
	{
		this.giftCode = giftCode;
		this.recipientName = recipientName;
		this.senderName = senderName;
		this.amount = amount;
		this.balance = balance;
		this.deliveredOn = deliveredOn;
	}
	@Override
	public Integer getType()
	{
		// TODO Auto-generated method stub
		return 4;
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

	public String getDeliveredOn() {
		return deliveredOn;
	}

	public static class Builder {

		private String giftCode;

		private String recipientName;

		private String senderName;

		private BigDecimal amount;

		private BigDecimal balance;

		private String deliveredOn;
		
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
		
		public Builder deliveredOn(String deliveredOn)
		{
			this.deliveredOn = deliveredOn;
			return this;
		}
		
		public GiftDelivered build()
		{
			return new GiftDelivered(giftCode,recipientName, senderName,amount,balance,deliveredOn);
		}
	}

	

}
