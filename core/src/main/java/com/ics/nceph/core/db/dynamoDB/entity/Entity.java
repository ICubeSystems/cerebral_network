package com.ics.nceph.core.db.dynamoDB.entity;

import org.springframework.data.annotation.Id;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
/**
 * 
 * @author Chandan Verma
 * @since 18-Aug-2022
 */
@DynamoDBDocument // Indicates that a class can be serialized as an Amazon DynamoDB document
public class Entity 
{
	@Id
	private Key key;
	
	public Entity() {}
	
	public Entity(Key key) 
	{
		this.key = key;
	}
	
	public void setKey(Key key) 
	{
		this.key = key;
	}
	
	@DynamoDBHashKey(attributeName = "partitionKey")
	public String getPartitionKey() 
	{
		return key != null ? key.getPartitionKey() : null ;
	}

	public void setPartitionKey(String partitionKey) 
	{
		if(key == null) {
			key = new Key();
		}
		key.setPartitionKey(partitionKey);
	}
	
	@DynamoDBRangeKey(attributeName = "sortKey")
	public String getSortKey() 
	{
		return key != null ? key.getSortKey() : null ;
	}

	public void setSortKey(String sortKey) 
	{
		if(key == null) {
			key = new Key();
		}
		key.setSortKey(sortKey);
	}
}
