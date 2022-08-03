package com.ics.nceph.config;

import com.ics.nceph.core.message.data.ConfigData;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 28-Jul-2022
 */
public class ConfigStore 
{
	private static SynapticConfiguration configuration;
	
	private static Integer state = 100;

	/**
	 * This method is used to initialize and build the SynapticConfiguration cache on the node. This method is called vis configReceptor.
	 * 
	 */
	public static void init(ConfigData config) 
	{
		configuration = new SynapticConfiguration.Builder()
							.nodeId(config.getNodeId())
							.applicationReceptors(config.getEventReceptors())
							.build();
							
		// call the builder instead
		state = 200;
	}

	/**
	 * This method returns FQCN of the ApplicationReceptor class for an eventType
	 * 
	 * @param eventType
	 * @return String - FQCN of the ApplicationReceptor class for an eventType
	 * @version 1.0
	 */
	public static String getApplicationReceptor(Integer eventType)
	{
		String receptor = null;
		try
		{
			receptor = configuration.getApplicationReceptors().get(eventType);
		}catch (NullPointerException e){}
		return receptor;
	}
	
	public static Integer getNodeId() {
		return configuration.getNodeId();
	}
	
	public static boolean isReady()
	{
		return state == 100 ? false : true;
	}
}
