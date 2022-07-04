package com.ics.synapse.ncephEvent;

import com.ics.menu.GiftDelivered;
import com.ics.menu.GiftItem;
import com.ics.menu.GiftRedeem;
import com.ics.menu.GiftRefund;
import com.ics.menu.GiftStatus;
import com.ics.menu.eventThreads.EventThread;
import com.ics.menu.eventThreads.GiftDeliveredThread;
import com.ics.menu.eventThreads.GiftItemThread;
import com.ics.menu.eventThreads.GiftRedeemThread;
import com.ics.menu.eventThreads.GiftRefundThread;
import com.ics.menu.eventThreads.GiftStatusThread;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;

/**
 * 
 * @author Anshul
 * @since 21-Jun-2022
 *
 */
public class EventType {
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

	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final EventType GIFT_ITEM = new EventType(1000, GiftItem.class, GiftItemThread.class);
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final EventType GIFT_REDEEM = new EventType(2000, GiftRedeem.class, GiftRedeemThread.class);
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final EventType GIFT_REFUND = new EventType(3000, GiftRefund.class, GiftRefundThread.class);
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final EventType GIFT_DELIVERED = new EventType(4000, GiftDelivered.class, GiftDeliveredThread.class);
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final EventType GIFT_STATUS = new EventType(5000, GiftStatus.class, GiftStatusThread.class);
	/**
	 * 
	 */ 
	public static EventType[] types = new EventType[] {GIFT_ITEM, GIFT_REDEEM, GIFT_REFUND, GIFT_DELIVERED, GIFT_STATUS};
	
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
