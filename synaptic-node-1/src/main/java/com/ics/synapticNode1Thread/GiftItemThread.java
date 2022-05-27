package com.ics.synapticNode1Thread;

import java.math.BigDecimal;
import com.ics.synapse.Emitter;
import com.ics.synapticnode1.GiftItem;

/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */

public class GiftItemThread extends Thread
{
	public void run()
	{
		for (int i = 1; i <= 100; i++)
		{
			try
			{
				GiftItem gi = new GiftItem.Builder()
						.recipientName("Receiver " + i)
						.senderName("Sender " + i)
						.giftCode("570087975676400" + i)
						.amount(new BigDecimal(100.0))
						.balance(new BigDecimal(100.0))
						.build();

				Emitter.emit(gi.toEvent());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
