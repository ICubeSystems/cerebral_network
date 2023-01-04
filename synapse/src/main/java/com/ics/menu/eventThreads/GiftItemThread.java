package com.ics.menu.eventThreads;

import java.math.BigDecimal;

import com.ics.menu.GiftItem;
import com.ics.synapse.Emitter;

/**
 * 
 * @author Chandan Verma
 * @since 07-Mar-2022
 */

public class GiftItemThread extends EventThread
{
	public GiftItemThread(Integer totalEvents){
		super(totalEvents);
	}
	
	public void run()
	{
		for (int i = 1; i <= getNumberOfEvents(); i++)
		{
			try
			{
				GiftItem gi = new GiftItem.Builder()
						.recipientName("AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "Anshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ ""
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ ""
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul"
								+ "AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"Anshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul\"\r\n"
								+ "								+ \"AnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshulAnshul")
						.senderName("mukul")
						.giftCode("570087975676400" + i)
						.amount(new BigDecimal(100.0))
						.balance(new BigDecimal(100.0))
						.build();

				Emitter.emit(gi.toEvent(123));
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
