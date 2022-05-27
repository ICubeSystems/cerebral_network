package com.cs.synapticNode2Thread;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.ics.synapse.Emitter;
import com.ics.synapticnode2.GiftDelivered;
/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */

public class GiftDeliveredThread extends Thread
{
	public void run()
	{
			for( int i = 1; i <= 100; i++)
			{
				try {
					
					GiftDelivered giftDelivered = new GiftDelivered.Builder()
							.recipientName("Receiver "+i)
							.senderName("Sender "+i)
							.giftCode("5700879756760-D-"+i)
							.amount(new BigDecimal(100.0))
							.balance(new BigDecimal(100.0))
							.deliveredOn(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm")))
							.build();
					
					Emitter.emit(giftDelivered.toEvent());
					
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
	}
}
