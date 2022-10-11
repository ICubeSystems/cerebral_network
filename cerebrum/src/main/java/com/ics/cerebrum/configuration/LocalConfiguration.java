package com.ics.cerebrum.configuration;

import java.io.IOException;
import java.nio.file.Paths;

import com.ics.cerebrum.configuration.exception.ConfigurationException;
import com.ics.cerebrum.db.document.LocalNodeIdMap;
import com.ics.cerebrum.db.document.SynapticNodesList;
import com.ics.nceph.core.Configuration;

/**
 * Cerebral configuration deals with local database
 * @author Anshul
 * @version 1.0
 * @since Sep 27, 2022
 */
public class LocalConfiguration implements CerebralConfiguration
{
	@Override
	public Integer getNodeIdForKey(String secretKey) throws ConfigurationException
	{
		try
		{
			LocalNodeIdMap map = mapper.readValue(Paths.get(localDocumentStoreLocation()+"node.json").toFile(), LocalNodeIdMap.class);
			return map.get(secretKey);
		} catch (IOException e)
		{
			 throw new ConfigurationException("Application configuration error", e);
		}
	}

	@Override
	public SynapticNodesList getSynapticNodes() throws ConfigurationException
	{
		try
		{
			return mapper.readValue(Paths.get(localDocumentStoreLocation()+"application.json").toFile(), SynapticNodesList.class);
		} catch (IOException e)
		{
			throw new ConfigurationException("Application configuration error", e);
		}
	}
	
	private String localDocumentStoreLocation() 
	{
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.app_configutation");
	}
}
