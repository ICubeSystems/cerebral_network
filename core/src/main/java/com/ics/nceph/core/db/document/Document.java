package com.ics.nceph.core.db.document;

import org.springframework.data.annotation.Id;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
/**
 * 
 * @author Anshul
 * @since 18-Aug-2022
 */
@DynamoDBDocument
public class Document 
{
	@Id
	@DynamoDBIgnore
	private Key<String, String> key;
	
	public Document() 
	{
		if(key == null) 
			key = new Key<>();
	}
	
	public void setKey(Key<String, String> key)
	{
		this.key = key;
	}

	@DynamoDBHashKey
	public String getPartitionKey() 
	{
		return key.getPartitionKey();
	}

	public void setPartitionKey(String partitionKey) 
	{
		key.setPartitionKey(partitionKey);
	}

	@DynamoDBRangeKey
	public String getSortKey() 
	{
		return key.getSortKey();
	}

	public void setSortKey(String sortKey) 
	{
		key.setSortKey(sortKey);
	}
}
