package com.ics.cerebrum.db.repository;

import java.util.List;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ics.cerebrum.db.document.NodeConfiguration;
import com.ics.cerebrum.db.document.NetworkConfiguration;
import com.ics.nceph.core.db.document.Key;

/**
 * CRUD Repository interface for {@link NetworkConfiguration}.
 * 
 * @author Anshul
 * @since 31-Aug-2022
 */

@Repository
@EnableScan
public interface NodeConfigurationRepository extends CrudRepository<NodeConfiguration, Key> 
{
	List<NodeConfiguration> findAllByPartitionKey(String partitionKey);
	
	List<NodeConfiguration> findBySortKey(String sortKey);  
	
	NodeConfiguration findBysecretKey(String secretKey); 
}
