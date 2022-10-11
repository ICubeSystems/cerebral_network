package com.ics.nceph.core.db.document.store;

import com.ics.nceph.config.SynapticConfiguration;
import com.ics.nceph.core.message.data.ConfigData;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 28-Jul-2022
 */
public class ConfigStore 
{
	private SynapticConfiguration configuration;
	
	private static ConfigStore configStore;
	
	private Integer state = 100;

	/**
	 * This method is used to initialize and build the SynapticConfiguration cache on the node. This method is called vis configReceptor.
	 * 
	 */
	public void init(ConfigData config) 
	{
		configuration = new SynapticConfiguration.Builder()
							.nodeId(config.getNodeId())
							.applicationReceptors(config.getEventReceptors())
							.build();
							
		// call the builder instead
		state = 200;
	}
	public static ConfigStore getInstance()
	{
		if(configStore == null) {
			configStore = new ConfigStore();
    		System.out.println("ConfigData new instance");
    	}
    	return configStore;
	}
	/**
	 * This method returns FQCN of the ApplicationReceptor class for an eventType
	 * 
	 * @param eventType
	 * @return String - FQCN of the ApplicationReceptor class for an eventType
	 * @version 1.0
	 */
	public String getApplicationReceptor(Integer eventType)
	{
		String receptor = null;
		try
		{
			receptor = configuration.getApplicationReceptors().get(eventType);
		}catch (NullPointerException e){}
		return receptor;
	}
	
	public Integer getNodeId() {
		return configuration.getNodeId();
	}
	
	public boolean isReady()
	{
		return state == 100 ? false : true;
	}
}
