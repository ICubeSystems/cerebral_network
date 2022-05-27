package com.ics.synapticNode1Thread;

import com.ics.synapse.Emitter;
import com.ics.synapticnode1.GiftRedeem;
import java.time.LocalDateTime; // Import the LocalDateTime class
import java.time.format.DateTimeFormatter; // Import the DateTimeFormatter class
/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */
//LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm"))
public class GiftRedeemThread extends Thread
{
	public void run()
	{
		for (int i = 1; i <= 100; i++)
		{
			try {
				GiftRedeem giftRedeem = new GiftRedeem.Builder()
						.recipientName("Receiver " + i)
						.senderName("Sender " + i)
						.giftCode("570087975676432" + i)
						.redeemOn(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm")))
						.build();

				Emitter.emit(giftRedeem.toEvent());
			} catch (Exception e) 
			{
				e.printStackTrace();
			}
		}
	}
}
