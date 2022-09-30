package com.ics.nceph.core.message;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 15-Mar-2022
 */
@DynamoDBDocument
public class TimeRecord 
{
	@DynamoDBAttribute
	private long start;
	
	@DynamoDBAttribute
	private long end;
	
	public TimeRecord() {}
	
	public TimeRecord(long start, long end)
	{
		this.start = start;
		this.end = end;
	}
	
	public long getStart() {
		return start;
	}
	
	public long getEnd() {
		return end;
	}
}
