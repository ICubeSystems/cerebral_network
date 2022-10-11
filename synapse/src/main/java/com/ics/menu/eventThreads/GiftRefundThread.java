package com.ics.menu.eventThreads;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.ics.menu.GiftRefund;
import com.ics.synapse.Emitter;

/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */

public class GiftRefundThread extends EventThread
{
	public GiftRefundThread(Integer totalEvents){
		super(totalEvents);
	}
	
	public void run()
	{
			for (int i = 1; i <= getNumberOfEvents(); i++)
			{
				try {
					GiftRefund giftRefund = new GiftRefund.Builder()
							.recipientName("Receiver " + i)
							.senderName("Sender " + i)
							.giftCode("570087975676004" + i)
							.amount(new BigDecimal(150.00))
							.balance(new BigDecimal(150.00))
							.refundOn(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm")))
							.build();

					Emitter.emit(giftRefund.toEvent(123));
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
	}
}
