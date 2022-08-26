package com.ics.nceph.core.db.dynamoDB.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.ics.nceph.core.event.EventData;

/**
 * <b>MessageEntity</b> is the main entity of our db schema. MessageEntity entity is further specialized into:
 * <ol>
 * 	<li>{@link PublishedMessageEntity} - messages published by a node</li>
 * 	<li>{@link ReceivedMessageEntity} - messages received by a node</li>
 * </ol>
 * 
 * This is a base model class for <b>MessageEntity</b> entity. <br>
 * 
 * @author Chandan Verma
 * @since 05-Aug-2022
 */
@DynamoDBDocument // Indicates that a class can be serialized as an Amazon DynamoDB document
public class MessageEntity extends Entity
{
	private Integer producerPortNumber;
	
	private Integer producerNodeId;
	
	private Integer eventType;
	
	private Integer eventId;
	
	private String messageId;
	
	private String eventData;
	
	private long  createdOn;
	
	private TimeRecord  eventMessageWriteRecord;
	
	private TimeRecord eventMessageReadRecord;
	
	private TimeRecord ackMessageWriteRecord;
	
	private TimeRecord ackMessageReadRecord;
	
	private TimeRecord threeWayAckMessageWriteRecord;
	
	private TimeRecord threeWayAckMessageReadRecord;

	private TimeRecord threeWayAckMessageNetworkRecord;
	
	private TimeRecord eventMessageNetworkRecord;
	
	private TimeRecord ackMessageNetworkRecord;
	
	private int eventMessageAttempts = 0;

	private Integer acknowledgementMessageAttempts = 0;
	
	private Integer threeWayAckMessageAttempts = 0;

	private Integer finalMessageAttempts = 0;
	
	private MessageDeliveryState messageDeliveryState;
	
	public MessageEntity() {}
	
	public MessageEntity(Key key) 
	{
		super(key);
	}
	
	public void setEvent(EventData event) 
	{
		// MessageEntity event data
		this.eventData = event.getObjectJSON();
		// eventId
		this.eventId = event.getEventId();
		// eventType
		this.eventType = event.getEventType();
	}
	
	@DynamoDBAttribute(attributeName = "eventType")
	public Integer getEventType() 
	{
		return eventType;
	}

	public void setEventType(Integer eventType) 
	{
		this.eventType = eventType;
	}

	@DynamoDBAttribute(attributeName = "eventId")
	public Integer getEventId() 
	{
		return eventId;
	}

	public void setEventId(Integer eventId) 
	{
		this.eventId = eventId;
	}

	@DynamoDBAttribute(attributeName = "producerPortNumber")
	public Integer getProducerPortNumber() 
	{
		return producerPortNumber;
	}

	public void setProducerPortNumber(Integer producerPortNumber) 
	{
		this.producerPortNumber = producerPortNumber;
	}

	@DynamoDBAttribute(attributeName = "producerNodeId")
	public Integer getProducerNodeId() 
	{
		return producerNodeId;
	}

	public void setProducerNodeId(Integer producerNodeId) 
	{
		this.producerNodeId = producerNodeId;
	}
	
	@DynamoDBAttribute(attributeName = "eventData")
	public String getEventData() 
	{
		return eventData;
	}
	
	public void setEventData(String eventData) 
	{
		this.eventData = eventData;
	}

	@DynamoDBAttribute(attributeName = "acknowledgementMessageAttempts")
	public Integer getAcknowledgementMessageAttempts() 
	{
		return acknowledgementMessageAttempts;
	}

	public void setAcknowledgementMessageAttempts(Integer acknowledgementAttempts) 
	{
		this.acknowledgementMessageAttempts = acknowledgementAttempts;
	}

	@DynamoDBAttribute(attributeName = "threeWayAckMessageAttempts")
	public Integer getThreeWayAckMessageAttempts() 
	{
		return threeWayAckMessageAttempts;
	}

	public void setThreeWayAckMessageAttempts(Integer threeWayAckAttempts) 
	{
		this.threeWayAckMessageAttempts = threeWayAckAttempts;
	}

	@DynamoDBAttribute(attributeName = "createdOn")
	public long getCreatedOn() 
	{
		return createdOn;
	}

	public void setCreatedOn(long createdOn) 
	{
		this.createdOn = createdOn;
	}

	@DynamoDBAttribute(attributeName = "threeWayAckMessageWriteRecord")
	public TimeRecord getThreeWayAckMessageWriteRecord() 
	{
		return threeWayAckMessageWriteRecord;
	}

	public void setThreeWayAckMessageWriteRecord(TimeRecord threeWayAckWriteRecord) 
	{
		this.threeWayAckMessageWriteRecord = threeWayAckWriteRecord;
	}

	@DynamoDBAttribute(attributeName = "eventMessageReadRecord")
	public TimeRecord getReadRecord() 
	{
		return eventMessageReadRecord;
	}

	public void setReadRecord(TimeRecord readRecord) 
	{
		this.eventMessageReadRecord = readRecord;
	}

	@DynamoDBAttribute(attributeName = "eventMessageWriteRecord")
	public TimeRecord getEventMessageWriteRecord() 
	{
		return eventMessageWriteRecord;
	}

	public void setEventMessageWriteRecord(TimeRecord writeRecord) 
	{
		this.eventMessageWriteRecord = writeRecord;
	}

	@DynamoDBAttribute(attributeName = "eventMessageNetworkRecord")
	public TimeRecord getEventMessageNetworkRecord() 
	{
		return eventMessageNetworkRecord;
	}

	public void setEventMessageNetworkRecord(TimeRecord eventNetworkRecord) 
	{
		this.eventMessageNetworkRecord = eventNetworkRecord;
	}

	@DynamoDBAttribute(attributeName = "ackMessageNetworkRecord")
	public TimeRecord getAckMessageNetworkRecord() 
	{
		return ackMessageNetworkRecord;
	}

	public void setAckMessageNetworkRecord(TimeRecord ackNetworkRecord) 
	{
		this.ackMessageNetworkRecord = ackNetworkRecord;
	}

	@DynamoDBAttribute(attributeName = "threeWayAckMessageNetworkRecord")
	public TimeRecord getThreeWayAckMessageNetworkRecord() 
	{
		return threeWayAckMessageNetworkRecord;
	}

	public void setThreeWayAckMessageNetworkRecord(TimeRecord threeWayAckNetworkRecord) 
	{
		this.threeWayAckMessageNetworkRecord = threeWayAckNetworkRecord;
	}

	@DynamoDBAttribute(attributeName = "messageId")
	public String getMessageId() 
	{
		return messageId;
	}

	public void setMessageId(String messageId) 
	{
		this.messageId = messageId;
	}
	
	@DynamoDBAttribute(attributeName = "finalMessageAttempts")
	public Integer getFinalMessageAttempts() 
	{
		return finalMessageAttempts;
	}

	public void setFinalMessageAttempts(Integer finalMessageAttempts) 
	{
		this.finalMessageAttempts = finalMessageAttempts;
	}

	@DynamoDBAttribute(attributeName = "eventMessageAttempts")
	public int getEventMessageAttempts() 
	{
		return eventMessageAttempts;
	}

	public void setEventMessageAttempts(int eventPublishAttempts) 
	{
		this.eventMessageAttempts = eventPublishAttempts;
	}
	
	@DynamoDBAttribute(attributeName = "ackMessageWriteRecord")
	public TimeRecord getAckMessageWriteRecord() 
	{
		return ackMessageWriteRecord;
	}

	public void setAckMessageWriteRecord(TimeRecord ackWriteRecord) 
	{
		this.ackMessageWriteRecord = ackWriteRecord;
	}
	
	@DynamoDBAttribute(attributeName = "threeWayAckMessageReadRecord")
	public TimeRecord getThreeWayAckMessageReadRecord() 
	{
		return threeWayAckMessageReadRecord;
	}

	public void setThreeWayAckMessageReadRecord(TimeRecord threeWayAckReadRecord) 
	{
		this.threeWayAckMessageReadRecord = threeWayAckReadRecord;
	}
	
	@DynamoDBAttribute(attributeName = "ackMessageReadRecord")
	public TimeRecord getAckMessageReadRecord() 
	{ 
		return ackMessageReadRecord;
	}

	public void setAckMessageReadRecord(TimeRecord ackReadRecord) 
	{
		this.ackMessageReadRecord = ackReadRecord;
	}

	@DynamoDBAttribute(attributeName = "messageDeliveryState")
	public MessageDeliveryState getMessageDeliveryState() 
	{
		return messageDeliveryState;
	}

	public void setMessageDeliveryState(MessageDeliveryState messageDeliveryState) 
	{
		this.messageDeliveryState = messageDeliveryState;
	}
}
