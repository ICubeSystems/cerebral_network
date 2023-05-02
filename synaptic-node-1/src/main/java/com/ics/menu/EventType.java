package com.ics.menu;

import com.ics.menu.eventThreads.EventThread;
import com.ics.menu.eventThreads.GiftDeliveredThread;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.synapse.ncephEvent.Event;

/**
 * 
 * @author Anshul
 * @since 21-Jun-2022
 *
 */
public class EventType 
{
	private int type;
	
	private Class<? extends EventThread> callingThread;
	
	private Class<? extends Event> giftClass;
	
	EventType(int type, Class<? extends Event> giftClass, Class<? extends EventThread> callingThread)
	{
		this.type = type;
		this.giftClass = giftClass;
		this.callingThread = callingThread;
	}
	
	public int getType() {
		return type;
	}
	
	public Class<? extends Event> getGiftClass() {
		return giftClass;
	}
	
	public Class<? extends EventThread> getCallingclass() {
		return callingThread;
	}

//	public static final EventType GIFT_ITEM = new EventType(1, GiftItem.class, GiftItemThread.class);
//
//	public static final EventType GIFT_REDEEM = new EventType(2, GiftRedeem.class, GiftRedeemThread.class);
//	
//	public static final EventType GIFT_REFUND = new EventType(3, GiftRefund.class, GiftRefundThread.class);
	
	public static final EventType GIFT_DELIVERED = new EventType(4, GiftDelivered.class, GiftDeliveredThread.class);
	
//	public static final EventType GIFT_STATUS = new EventType(5, GiftStatus.class, GiftStatusThread.class);
	
	public static EventType[] types = new EventType[] {GIFT_DELIVERED};
	
	/**
	 * Returns the MessageType instance by the type supplied
	 * 
	 * @param type
	 * @throws InvalidMessageTypeException
	 * @return MessageType
	 */
	public static EventType getEventType(int type)
	{
		for (EventType giftType : types) 
			if(giftType.getType() == type)
				return giftType;
		System.out.println("Invalid gift type");
		return null;
	}
	
}
