package com.ics.cerebrum.configuration;

import org.springframework.beans.factory.annotation.Autowired;

import com.ics.cerebrum.db.repository.ApplicationConfigurationRepository;
import com.ics.cerebrum.db.repository.NodeConfigurationRepository;
import com.ics.nceph.NcephConstants;

/**
 * This class is used to initialize cerebral configuration
 * @author Anshul
 * @version 1.0
 * @since Sep 27, 2022
 */
public class CerebralConfigurationIntializer
{
	@Autowired
	ApplicationConfigurationRepository applicationConfigurationRepository;

	@Autowired
	NodeConfigurationRepository nodeConfigurationRepository;
	
	public CerebralConfiguration initialiseCerebralConfiguration()
	{
		if(NcephConstants.saveInDB) 
			 return new DynamoDBConfiguration(applicationConfigurationRepository, nodeConfigurationRepository);
		else 
			 return new LocalConfiguration();
	}
}
