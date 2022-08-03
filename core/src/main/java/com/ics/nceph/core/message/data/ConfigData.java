package com.ics.nceph.core.message.data;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Message data for {@link ConfigMessage}. This message contains the following data:<br>
 * <ol>
 * 	<li>NodeId - unique identifier of a synaptic node in the nceph network</li>
 * 	<li>Map of EventType and ApplicationReceptor</li>
 * </ol>
 * 
 * @author Anshul
 * @since 28-Jul-2022
 */
public class ConfigData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * NodeId - unique identifier of a synaptic node in the nceph network
	 */
	public Integer nodeId;
	
	/**
	 * Map of EventType and ApplicationReceptor
	 */
	public HashMap<Integer, String> eventReceptors;
	
	public ConfigData() {}
	
	public ConfigData(Integer nodeId, HashMap<Integer, String> eventreceptors) {
		super();
		this.nodeId = nodeId;
		this.eventReceptors = eventreceptors;
	}
	
	/**
	 * Get NodeId
	 * @return NodeId - unique identifier of a synaptic node in the nceph network
	 */
	public Integer getNodeId() {
		return nodeId;
	}

	/**
	 * Map of EventType and ApplicationReceptor
	 * @return HashMap
	 */
	public HashMap<Integer, String> getEventReceptors() {
		return eventReceptors;
	}

	public static class Builder
	{
		private Integer nodeId;
		
		public HashMap<Integer, String> eventReceptors;
		
		public Builder nodeId(Integer nodeId) {
			this.nodeId = nodeId;
			return this;
		}
		
		public Builder eventReceptors(HashMap<Integer, String> eventreceptors) {
			this.eventReceptors = eventreceptors;
			return this;
		}
		
		public ConfigData build() 
		{
			return new ConfigData(this.nodeId, this.eventReceptors);
		}
	}
}