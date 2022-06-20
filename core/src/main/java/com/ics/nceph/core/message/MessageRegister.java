package com.ics.nceph.core.message;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author Anshul
 * @since 17-Jun-2022
 */
public class MessageRegister 
{
	/**
	 * 
	 */
	private ConcurrentHashMap<Integer, Set<Long>> register;
	
	public MessageRegister()
	{
		this.register = new ConcurrentHashMap<Integer, Set<Long>>();
	}
	
	/**
	 * 
	 * @param message
	 */
	public void add(Message message) 
	{
		// Message register should only store event messages (no other message types should be stored)
		if (message.decoder().getType() == 0x0B || message.decoder().getType() == 0x03)
		{
			// get the messageIds stored for a particular node
			Set<Long> messageIds = register.get(message.decoder().getSourceId());
			// If there are no messaged for the node/ source then create a new hash set and put it inside the register
			if (messageIds == null)
			{
				messageIds = Collections.synchronizedSet(new HashSet<Long>());
				register.put(message.decoder().getSourceId(), messageIds);
			}
			// Add the message id in the hash set. If the messageId is duplicate then it will not save it again. 
			// DUPLICACY CHECKED
			synchronized (messageIds) {
				messageIds.add(message.decoder().getMessageId());
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
			// get the messageIds stored for a particular node
			Set<Long> messageIds = register.get(message.decoder().getSourceId());
			synchronized (messageIds) {
				messageIds.remove(message.decoder().getMessageId());
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
		Set<Long> messageIds = register.get(message.decoder().getSourceId());
		if (messageIds == null)
			return false;
		return messageIds.contains(message.decoder().getMessageId());
	}
}
