package com.ics.id;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.ics.nceph.core.Configuration;

/**
 * 
 * @author Anshul
 * @since 29-Jun-2022
 */
public class IdCounter
{
	private ConcurrentHashMap<Integer, AtomicLong> sequenceCounters;
	
	public IdCounter() 
	{
		sequenceCounters = new ConcurrentHashMap<Integer, AtomicLong>();
	}

	public ConcurrentHashMap<Integer, AtomicLong> getSequenceCounters() {
		return sequenceCounters;
	}

	public String localIdCounterLocation() {
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.id_location");
	}
	
	public String IdCounterFileName() {
		return "idCounter.json";
	}
}
