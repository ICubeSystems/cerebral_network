package com.ics.nceph.core.db.dynamoDB.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

/**
 * 
 * @author Chandan Verma
 * @since 19-Aug-2022
 */

@DynamoDBDocument // Indicates that a class can be serialized as an Amazon DynamoDB document
public class TimeRecord 
{
	private long start;

	private long end;

	public TimeRecord() {}

	public TimeRecord(long start, long end)
	{
		this.start = start;
		this.end = end;
	}

	@DynamoDBAttribute(attributeName = "start")
	public long getStart() {
		return start;
	}

	@DynamoDBAttribute(attributeName = "end")
	public long getEnd() {
		return end;
	}

	public void setStart(long start) 
	{
		this.start = start;
	}

	public void setEnd(long end) 
	{
		this.end = end;
	}
}