package com.ics.nceph.core.db.repository;

import org.socialsignin.spring.data.dynamodb.repository.DynamoDBCrudRepository;
import org.springframework.stereotype.Repository;

import com.ics.nceph.core.db.document.DocumentList;
import com.ics.nceph.core.db.document.Key;
import com.ics.nceph.core.db.document.ProofOfPublish;

/**
 * CRUD Repository interface for {@link PublishedMessageEntity}.
 * 
 * @author Chandan Verma
 * @since 5-Aug-2022
 */
@Repository
public interface PublishedMessageRepository extends DynamoDBCrudRepository<ProofOfPublish, Key<String, String>> 
{
	/**
	 * Used to get list of {@link ProofOfPublish} type documents using partitionKey
	 * @param partitionKey
	 * @return
	 * @version 1.0
	 * @since Sep 28, 2022
	 */
	DocumentList<ProofOfPublish> findAllByPartitionKey(String partitionKey);

	/**
	 * Used to get list of {@link ProofOfPublish} type documents using action and message delivery state
	 * @param action
	 * @param messageDeliveryState
	 * @return
	 * @version 1.0
	 * @since Sep 28, 2022
	 */
	DocumentList<ProofOfPublish> findAllByActionAndMessageDeliveryStateLessThan(String action, Integer messageDeliveryState);

}