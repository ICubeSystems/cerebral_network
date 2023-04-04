package com.ics.nceph.core.db.repository;

import org.socialsignin.spring.data.dynamodb.repository.DynamoDBCrudRepository;
import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.stereotype.Repository;

import com.ics.nceph.core.db.document.DocumentList;
import com.ics.nceph.core.db.document.Key;
import com.ics.nceph.core.db.document.ProofOfRelay;

/**
 * CRUD Repository interface for {@link ReceivedMessageEntity}
 * @author Anshul
 * @since 5-Aug-2022
 */

@Repository
@EnableScan
public interface ReceivedMessageRepository extends DynamoDBCrudRepository<ProofOfRelay, Key<String, String>> 
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
	 * Used to get list of {@link ProofOfRelay} type documents using action and message delivery stat
	 * @version V_6_0
	 * @since Mar 29, 2023
	 * @param action
	 * @param messageDeliveryState
	 * @return
	 */
	DocumentList<ProofOfRelay> findAllByActionAndMessageDeliveryStateLessThan(String action, Integer messageDeliveryState);
}
