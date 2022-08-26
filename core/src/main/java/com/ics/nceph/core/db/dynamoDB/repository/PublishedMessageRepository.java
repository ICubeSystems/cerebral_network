package com.ics.nceph.core.db.dynamoDB.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ics.nceph.core.db.dynamoDB.entity.Key;
import com.ics.nceph.core.db.dynamoDB.entity.PublishedMessageEntity;

/**
 * CRUD Repository interface for {@link PublishedMessageEntity}.
 * 
 * @author Chandan Verma
 * @since 5-Aug-2022
 */

@Repository
@EnableScan
public interface PublishedMessageRepository extends CrudRepository<PublishedMessageEntity, Key> 
{
	
}
