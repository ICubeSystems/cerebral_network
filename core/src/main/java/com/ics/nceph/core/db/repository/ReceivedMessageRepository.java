package com.ics.nceph.core.db.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ics.nceph.core.db.document.DocumentList;
import com.ics.nceph.core.db.document.Key;
import com.ics.nceph.core.db.document.ProofOfRelay;

/**
 * CRUD Repository interface for {@link ReceivedMessageEntity}
 * 
 * @author Chandan Verma
 * @since 5-Aug-2022
 */

@Repository
@EnableScan
public interface ReceivedMessageRepository extends CrudRepository<ProofOfRelay, Key> 
{
	/**
	 * Used to get list of {@link ProofOfRelay} type documents using partitionKey
	 * @param PartitionKey
	 * @return
	 * @version 1.0
	 * @since Sep 28, 2022
	 */
	DocumentList<ProofOfRelay> findAllByPartitionKey(String PartitionKey);
	
	/**
	 * Used to get list of {@link ProofOfRelay} type documents using prefix, producerPort, messageDeliveryState
	 * @param prefix
	 * @param producerPort
	 * @param messageDeliveryState
	 * @return
	 * @version 1.0
	 * @since Sep 28, 2022
	 */
	DocumentList<ProofOfRelay> findAllByPartitionKeyStartingWithAndProducerPortNumberAndMessageDeliveryStateLessThan(String prefix, Integer producerPort, Integer messageDeliveryState);
}
