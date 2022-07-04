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
	
	public MasterMessageLedger()
	{
		this.masterLedger = new ConcurrentHashMap<Integer, MessageLedger>();
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
			// get the messageIds stored for a particular node
			// If there are no messaged for the node/ source then create a new hash set and put it inside the register
			MessageLedger messageLedger = masterLedger.get(message.decoder().getSourceId());
			if (messageLedger == null)
			{
				messageLedger = new MessageLedger();
				masterLedger.put(message.decoder().getSourceId(), messageLedger);
			}
			// Add the message id in the hash set. If the messageId is duplicate then it will not save it again. 
			// DUPLICACY CHECKED
			synchronized (messageLedger) {
				messageLedger.add(message);
			}
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
	
	public int size() {
		int masterLedgerSize = 0;
		for (ConcurrentHashMap.Entry<Integer, MessageLedger> entry : masterLedger.entrySet()) {
			MessageLedger ledger = entry.getValue();
			masterLedgerSize += ledger.size();
		}
		return masterLedgerSize;
	}

	public ConcurrentHashMap<Integer, MessageLedger> getMasterLedger() {
		return masterLedger;
	}
}
