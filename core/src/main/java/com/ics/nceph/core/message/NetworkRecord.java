package com.ics.nceph.core.message;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Getter;
import lombok.Setter;

/**
 * This class records the time taken by the message to transmit completely from source to destination. It gives a measure of latency for a message in the network.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 15-Mar-2022
 */
@Getter
@Setter
@DynamoDBDocument
public class NetworkRecord
{
	@DynamoDBAttribute
	private long start;
	
	@DynamoDBAttribute
	private long end;
	
	public NetworkRecord() {}

	public NetworkRecord(long start, long end) 
	{
		this.start = start;
		this.end = end;
	}

	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 15-Mar-2022
	 */
	public static class Builder
	{
		long start;
		
		long end;
		
		public Builder start(long start)
		{
			this.start = start;
			return this;
		}
		
		public Builder end(long end)
		{
			this.end = end;
			return this;
		}
	
		public NetworkRecord build() 
		{
			return new NetworkRecord(start, end);
		}
	}
}
