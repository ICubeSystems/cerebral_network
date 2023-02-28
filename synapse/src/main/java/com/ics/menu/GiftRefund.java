package com.ics.menu;

import java.math.BigDecimal;

import com.ics.synapse.ncephEvent.Event;
/**
 * 
 * @author Chandan Verma
 * @since 04-Mar-2022
 */

public class GiftRefund extends Event
{

	private static final long serialVersionUID = -3996054630188026396L;
	
	private String giftCode;

	private String recipientName;

	private String senderName;

	private BigDecimal amount;

	private BigDecimal balance;

	private String refundOn;
	
	public GiftRefund(String giftCode, String recipientName,String senderName,BigDecimal amount,BigDecimal balance,String refundOn)
	{
		this.giftCode = giftCode;
		this.senderName = senderName;
		this.recipientName = recipientName;
		this.amount = amount;
		this.balance = balance;
		this.refundOn = refundOn;
	}
	
	@Override
	public Integer getType()
	{
		// TODO Auto-generated method stub
		return 3;
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

	public BigDecimal getAmount()
	{
		return amount;
	}

	public BigDecimal getBalance()
	{
		return balance;
	}

	public String getrefundOn()
	{
		return refundOn;
	}

	public static class Builder
	{
		private String giftCode;

		private String recipientName;

		private String senderName;

		private BigDecimal amount;

		private BigDecimal balance;

		private String refundOn;
		
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
		
		public Builder refundOn(String refundOn)
		{
			this.refundOn = refundOn;
			return this;
		}
		
		public GiftRefund build()
		{
			return new GiftRefund(giftCode, recipientName, senderName, amount, balance, refundOn);
		}
	}
}
