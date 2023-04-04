package com.ics.cerebrum.configuration;

import com.ics.cerebrum.configuration.exception.ConfigurationException;
import com.ics.cerebrum.db.document.SynapticNodesList;
import com.ics.cerebrum.db.repository.ApplicationConfigurationRepository;
import com.ics.cerebrum.db.repository.NodeConfigurationRepository;

/**
 * Cerebral configuration deals with cloud database
 * @author Anshul
 * @version 1.0
 * @since Sep 27, 2022
 */
public class DynamoDBConfiguration implements CerebralConfiguration
{
	/**
	 *  DB repository using to deal with Application configuration data.
	 */
	private ApplicationConfigurationRepository applicationConfigurationRepository;
	
	/**
	 *  DB repository using to deal with node configuration data.
	 */
	private NodeConfigurationRepository nodeConfigurationRepository;
	
	/**
	 * DynamoDBConfiguration constructor
	 * @param applicationConfigurationRepository
	 * @param nodeConfigurationRepository
	 */
	public DynamoDBConfiguration(ApplicationConfigurationRepository applicationConfigurationRepository, NodeConfigurationRepository nodeConfigurationRepository) {
		this.applicationConfigurationRepository = applicationConfigurationRepository;
		this.nodeConfigurationRepository = nodeConfigurationRepository;
	}
	
	@Override
	public Integer getNodeIdForKey(String secretKey) throws ConfigurationException
	{
		return nodeConfigurationRepository.findBysecretKey(secretKey).getNodeId();
	}

	@Override
	public SynapticNodesList getSynapticNodes() throws ConfigurationException
	{
		try
		{
			return applicationConfigurationRepository.findAllByPartitionKey("Config:App");
		} catch (Exception e)
		{
			throw new ConfigurationException(e);
		}
	}
}
