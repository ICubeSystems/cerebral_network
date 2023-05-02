package com.ics.nceph.core.message.data;

import java.io.Serializable;
import java.util.HashMap;

import com.ics.nceph.core.db.document.ApplicationReceptorMetaData;

import lombok.Getter;

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
@Getter
public class ConfigData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;

	/**
	 * NodeId - unique identifier of a synaptic node in the nceph network
	 */
	private Integer nodeId;
	
	private long messageCount;
	/**
	 * Map of EventType and ApplicationReceptor
	 */
	private HashMap<Integer, ApplicationReceptorMetaData> receptorMetaData;
	
	private String error;
	
	public ConfigData() {}
	
	public ConfigData(Integer nodeId, HashMap<Integer, ApplicationReceptorMetaData> eventreceptors, long messageCount, String error) {
		super();
		this.nodeId = nodeId;
		this.receptorMetaData = eventreceptors;
		this.messageCount = messageCount;
		this.error = error;
	}
	
	

	public static class Builder
	{
		private Integer nodeId;
		
		private HashMap<Integer, ApplicationReceptorMetaData> receptorMetaData;
		
		private String error;
		
		private long messageCount;
		
		public Builder nodeId(Integer nodeId) {
			this.nodeId = nodeId;
			return this;
		}
		
		public Builder receptorMetaData(HashMap<Integer, ApplicationReceptorMetaData> receptorMetaData) {
			this.receptorMetaData = receptorMetaData;
			return this;
		}
		
		public Builder messageCount(long messageCount) {
			this.messageCount = messageCount;
			return this;
		}
		
		public Builder error(String error) {
			this.error = error;
			return this;
		}
		
		public ConfigData build() 
		{
			return new ConfigData(this.nodeId, this.receptorMetaData, this.messageCount, this.error);
		}
	}
}