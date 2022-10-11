package com.ics.nceph.core.db.document;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;

import lombok.Getter;
import lombok.Setter;

/**
 * This Class represents a composite primary key for a {@link Document}
 *  
 * @author Chandan Verma
 * @since 18-Aug-2022
 */
@Getter
@Setter
@DynamoDBDocument //Indicates that a class can be serialized as an Amazon DynamoDB document
public class Key 
{
	@DynamoDBHashKey
	private String partitionKey;

	@DynamoDBRangeKey
	private String sortKey;

	public Key()
	{}

	public Key(String partitionKey, String sortKey)
	{
		this.partitionKey = partitionKey;
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
