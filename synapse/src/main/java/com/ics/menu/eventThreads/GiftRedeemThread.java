package com.ics.menu.eventThreads;

import java.time.LocalDateTime; // Import the LocalDateTime class
import java.time.format.DateTimeFormatter; // Import the DateTimeFormatter class

import com.ics.menu.GiftRedeem;
import com.ics.synapse.Emitter;
/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */
//LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm"))
public class GiftRedeemThread extends EventThread
{
	public GiftRedeemThread(Integer totalEvents){
		super(totalEvents);
	}
	
	public void run()
	{
		for (int i = 1; i <= getNumberOfEvents(); i++)
		{
			try {
				GiftRedeem giftRedeem = new GiftRedeem.Builder()
						.recipientName("Receiver " + i)
						.senderName("Sender " + i)
						.giftCode("570087975676432" + i)
						.redeemOn(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm")))
						.build();

				Emitter.emit(giftRedeem.toEvent(123));
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
}
