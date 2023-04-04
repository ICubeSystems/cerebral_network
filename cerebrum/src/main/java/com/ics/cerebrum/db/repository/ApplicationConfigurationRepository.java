package com.ics.cerebrum.db.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ics.cerebrum.db.document.NetworkConfiguration;
import com.ics.cerebrum.db.document.SynapticNodesList;
import com.ics.nceph.core.db.document.Key;

/**
 * CRUD Repository interface for {@link NetworkConfiguration}.
 * 
 * @author Anshul
 * @since 31-Aug-2022
 */
@Repository
@EnableScan
public interface ApplicationConfigurationRepository extends CrudRepository<NetworkConfiguration, Key<String, String>> 
{
	SynapticNodesList findAllByPartitionKey(String partitionKey);

	SynapticNodesList findByName(String messageId);
}
