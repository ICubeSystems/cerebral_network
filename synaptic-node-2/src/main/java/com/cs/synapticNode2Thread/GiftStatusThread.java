package com.cs.synapticNode2Thread;

import com.ics.synapse.Emitter;
import com.ics.synapticnode2.GiftStatus;
/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */

public class GiftStatusThread extends Thread
{
	public void run()
	{
			for(int i = 1; i <= 100; i++)
			{
				try {
					GiftStatus giftStatus = new GiftStatus.Builder()
							.recipientName("Receiver "+i)
							.senderName("Sender "+i)
							.giftCode("5700879756760"+i)
							.sendOnEmail(i+"UserEmail@email.com")
							.sendOnMobile("9876543210")
							.build();
					
					Emitter.emit(giftStatus.toEvent());
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
	}
}
