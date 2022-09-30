package com.ics.menu.eventThreads;

import com.ics.menu.GiftStatus;
import com.ics.synapse.Emitter;
/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */

public class GiftStatusThread extends EventThread
{
	public GiftStatusThread(Integer totalEvents){
		super(totalEvents);
	}
	
	public void run()
	{
			for(int i = 1; i <= getNumberOfEvents(); i++)
			{
				try {
					GiftStatus giftStatus = new GiftStatus.Builder()
							.recipientName("Receiver "+i)
							.senderName("Sender "+i)
							.giftCode("5700879756760"+i)
							.sendOnEmail(i+"UserEmail@email.com")
							.sendOnMobile("9876543210")
							.build();
					
					Emitter.emit(giftStatus.toEvent(123));
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
	}
}
