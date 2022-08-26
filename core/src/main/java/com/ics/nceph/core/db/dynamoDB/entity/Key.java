package com.ics.nceph.core.db.dynamoDB.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;

/**
 * This Class represents a composite primary key for a dynamoDB table
 *  
 * @author Chandan Verma
 * @since 18-Aug-2022
 */
@DynamoDBDocument //Indicates that a class can be serialized as an Amazon DynamoDB document
public class Key 
{
	private String partitionKey;

	private String sortKey;

	public Key()
	{}

	public Key(String partitionKey, String sortKey)
	{
		this.partitionKey = partitionKey;
		this.sortKey = sortKey;
	}

	/**
	 * This method returns Partition Key of the table
	 * 
	 * @return String value of the Partition Key
	 */
	@DynamoDBHashKey(attributeName = "partitionKey")
	public String getPartitionKey() 
	{
		return partitionKey;
	}

	/**
	 * This method sets the value of the Partition Key
	 * 
	 * @param partitionKey
	 */
	public void setPartitionKey(String partitionKey) 
	{
		this.partitionKey = partitionKey;
	}

	/**
	 * This method returns Sort Key of the table
	 * 
	 * @return String value of the Sort Key
	 */
	@DynamoDBRangeKey(attributeName = "sortKey")
	public String getSortKey() 
	{
		return sortKey;
	}

	/**
	 * This method sets the value of the Sort Key
	 * 
	 * @param SortKey
	 */
	public void setSortKey(String sortKey) 
	{
		this.sortKey = sortKey;
	}
	
	/**
	 * Builder class for composite key
	 *  
	 * @author chandan
	 */
	public static class Builder
	{
		private String partitionKey;
		
		private String sortKey;
		
		public Builder partitionKey(String partitionKey)
		{
			this.partitionKey = partitionKey;
			return this;
		}

		public Builder sortKey(String sortKey)
		{
			this.sortKey = sortKey;
			return this;
		}
		
		public Key build()
		{
			return new Key(partitionKey, sortKey);
		}
	}
}
