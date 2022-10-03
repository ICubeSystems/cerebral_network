package com.ics.nceph.core.message;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.event.EventData;

/**
 * This class stores messageIds per event type
 * 
 * @author Anshul
 * @since 17-Jun-2022
 */
public class MessageLedger 
{
	/**
	 * Hashmap with key as event type and value as Set of messageIds
	 */
	private ConcurrentHashMap<Integer, Set<Long>> ledger;
	
	private long messageCounter;
	
	//private long messageIdCounter;
	
	public MessageLedger()
	{
		this.ledger = new ConcurrentHashMap<Integer, Set<Long>>();
		messageCounter = 1;
	}
	
	/**
	 * 
	 * @param message
	 * @throws JsonProcessingException 
	 */
	public void add(Message message)
	{
		// Message register should only store event messages (no other message types should be stored)
		if (message.decoder().getType() == 0x0B || message.decoder().getType() == 0x03)
		{
			
		}
	}
	
	public void add(Integer eventType, long messageId) {
		// get the messageIds stored for a particular node
					Set<Long> messageIds = ledger.get(eventType);
					// If there are no messaged for the node/ source then create a new hash set and put it inside the register
					if (messageIds == null)
					{
						messageIds = Collections.synchronizedSet(new HashSet<Long>());
						ledger.put(eventType, messageIds);
					}
					// Add the message id in the hash set. If the messageId is duplicate then it will not save it again. 
					// DUPLICACY CHECKED
					synchronized (messageIds) {
						messageIds.add(messageId);
						if(messageCounter < messageId)
							messageCounter = messageId;
					}
	}
	
	/**
	 * Removes the message from the register
	 * @param message
	 */
	public synchronized void remove(Message message)
	{
		if (message.decoder().getType() == 0x0B || message.decoder().getType() == 0x03)
		{
			try {
				EventData eventData = (EventData) message.decoder().getData(EventData.class);
				// get the messageIds stored for a particular node
				Set<Long> messageIds = ledger.get(eventData.getEventType());
				synchronized (messageIds) {
					messageIds.remove(message.decoder().getMessageId());
				}
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param message
	 * @return
	 */
	public boolean contains(Message message) 
	{
		try 
		{
			EventData eventData = (EventData) message.decoder().getData(EventData.class);
			Set<Long> messageIds = ledger.get(eventData.getEventType());
			if (messageIds == null)
				return false;
			return messageIds.contains(message.decoder().getMessageId());
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			return false;
		}
	}
	
	public long messageCounter() {
		return messageCounter;
	}
	
	public int size() {
		int messageLedgerSize = 0;
		for (ConcurrentHashMap.Entry<Integer, Set<Long>> entry : ledger.entrySet()) {
			Set<Long> set = entry.getValue();
			messageLedgerSize += set.size();
		}
		return messageLedgerSize;
	}

	public ConcurrentHashMap<Integer, Set<Long>> getLedger() {
		return ledger;
	}
}
