package com.ics.nceph.core.db.dynamoDB.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

@DynamoDBDocument // Indicates that a class can be serialized as an Amazon DynamoDB document
public class MessageDeliveryState 
{
	private int state;
	
	public MessageDeliveryState(){}
	
	public MessageDeliveryState(int state)
	{
		this.state = state;
	}
	
	public void setState(int state) 
	{
		this.state = state;
	}
	
	@DynamoDBAttribute(attributeName = "state")
	public int getState() 
	{
		return state;
	}
}
