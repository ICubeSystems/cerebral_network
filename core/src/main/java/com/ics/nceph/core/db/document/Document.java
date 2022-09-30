package com.ics.nceph.core.db.document;

import org.springframework.data.annotation.Id;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
/**
 * 
 * @author Chandan Verma
 * @since 18-Aug-2022
 */
@DynamoDBTable(tableName = "Message")
public class Document 
{
	@Id
	private Key key;
	
	public Document() 
	{
		if(key == null) 
			key = new Key();
	}
	
	public void setKey(Key key) 
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
