package com.ics.nceph.core.message;

import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * This class stores MessageLedger per sourceId
 * 
 * @author Anshul
 * @since 17-Jun-2022
 */
public class MasterMessageLedger 
{
	/**
	 * 
	 */
	private ConcurrentHashMap<Integer, MessageLedger> masterLedger;

	private ConcurrentHashMap<Integer, Long> authMessageIdCounter;

	public MasterMessageLedger()
	{

		this.masterLedger = new ConcurrentHashMap<Integer, MessageLedger>();
		this.authMessageIdCounter = new ConcurrentHashMap<Integer, Long>();
	}

	/**
	 * 
	 * @param message
	 * @throws JsonProcessingException 
	 */
	public void add(Message message) 
	{
		// Message register should only store event messages (no other message types should be stored)
		if (message.decoder().getType() == 11 || message.decoder().getType() == 3)
			add(message.decoder().getSourceId(), message.decoder().geteventType(),message.decoder().getMessageId());
		else if (message.decoder().getType() == 0)
		{
			if (authMessageIdCounter.get(message.decoder().getSourceId()) == null // If there is no entry for the node then create an entry
					|| authMessageIdCounter.get(message.decoder().getSourceId()) < message.decoder().getMessageId()) // if the entry for the node has a smaller message id then update it with new message id
				authMessageIdCounter.put(message.decoder().getSourceId(), message.decoder().getMessageId());
		}
	}

	public void add(Integer sourceId, Integer eventType, long messageId) {
		// get the messageIds stored for a particular node
		// If there are no messaged for the node/ source then create a new hash set and put it inside the register
		MessageLedger messageLedger = masterLedger.get(sourceId);
		if (messageLedger == null)
		{
			messageLedger = new MessageLedger();
			masterLedger.put(sourceId, messageLedger);
		}
		// Add the message id in the hash set. If the messageId is duplicate then it will not save it again. 
		// DUPLICACY CHECKED
		synchronized (messageLedger) {
			messageLedger.add(eventType, messageId);
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
			MessageLedger messageLedger = masterLedger.get(message.decoder().getSourceId());
			// get the messageLedger stored for a particular node
			synchronized (messageLedger) {
				messageLedger.remove(message);
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
		MessageLedger messageLedger = masterLedger.get(message.decoder().getSourceId());
		if (messageLedger == null)
			return false;
		return messageLedger.contains(message);
	}

	public int size() 
	{
		int masterLedgerSize = 0;
		for (ConcurrentHashMap.Entry<Integer, MessageLedger> entry : masterLedger.entrySet()) {
			MessageLedger ledger = entry.getValue();
			masterLedgerSize += ledger.size();
		}
		return masterLedgerSize;
	}

	public long messageCount(Integer sourceId) 
	{
		try
		{
			return masterLedger.get(sourceId).messageCounter();
		} catch (NullPointerException e){return 0;}
	}

	public ConcurrentHashMap<Integer, MessageLedger> getMasterLedger() {
		return masterLedger;
	}
}
