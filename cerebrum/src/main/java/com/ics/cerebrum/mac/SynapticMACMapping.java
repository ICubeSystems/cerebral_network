package com.ics.cerebrum.mac;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ics.nceph.core.Configuration;

/**
 * Model class for synaptic node name resolution. The resolution is done via MAC address registry.
 * 
 * @author Anshul
 * @since 28-Jul-2022
 */
public class SynapticMACMapping
{
	private ConcurrentHashMap<String, Integer> macMapping;
	
	private AtomicInteger lastUsedNodeId;
	
	public SynapticMACMapping() 
	{
		lastUsedNodeId = new AtomicInteger(0);
		macMapping = new ConcurrentHashMap<String, Integer>();
	}

	public AtomicInteger getLastUsedNodeId() {
		return lastUsedNodeId;
	}

	public ConcurrentHashMap<String, Integer> getMacMapping() {
		return macMapping;
	}
	
	public String synapticMappingLocation() {
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.mac_map_location");
	}
	
	public String synapticMappingFileName() {
		return "SynapticMapping.json";
	}
}