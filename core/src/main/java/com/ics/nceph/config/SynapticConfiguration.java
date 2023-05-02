package com.ics.nceph.config;

import java.util.HashMap;

import com.ics.nceph.core.db.document.ApplicationReceptorMetaData;

/**
 * Model class for synaptic configurations. These configurations are sent by cerebrum in response to the {@link BootstrapMessage} sent by the synapse.<br>
 * This class contains the following configurations:<br>
 * <ol>
 * 	<li>NodeId - unique identifier of a synaptic node in the nceph network</li>
 * 	<li>Map of EventType and ApplicationReceptor</li>
 * </ol>
 * 
 * @author Anshul
 * @since 28-Jul-2022
 */
public class SynapticConfiguration
{
	// NodeId - unique identifier of a synaptic node in the nceph network
	private Integer nodeId;
	// Map of EventType and ApplicationReceptor
	private HashMap<Integer, ApplicationReceptorMetaData> applicationReceptorMetaData;

	public SynapticConfiguration(Integer nodeId, HashMap<Integer, ApplicationReceptorMetaData> applicationReceptorMetaData) 
	{
		this.nodeId = nodeId;
		this.applicationReceptorMetaData = applicationReceptorMetaData;
	}

	public HashMap<Integer, ApplicationReceptorMetaData> getApplicationReceptorMetaData() {
		return applicationReceptorMetaData;
	}

	public Integer getNodeId() {
		return nodeId;
	}
	/**
	 * Builder class
	 * 
	 * @author Anshul
	 * @since 28-Jul-2022
	 */
	public static class Builder
	{
		//NodeId - unique identifier of a synaptic node in the nceph network
		private Integer nodeId;
		// Map of EventType and ApplicationReceptor
		private HashMap<Integer, ApplicationReceptorMetaData> applicationReceptorMetaData;

		public Builder applicationReceptors(HashMap<Integer, ApplicationReceptorMetaData> applicationReceptorMetaData) {
			this.applicationReceptorMetaData = applicationReceptorMetaData;
			return this;
		}
		
		public Builder nodeId(Integer nodeId) {
			this.nodeId = nodeId;
			return this;
		}

		public SynapticConfiguration build() 
		{
			return new SynapticConfiguration(this.nodeId, this.applicationReceptorMetaData);
		}
	}
}