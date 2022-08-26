package com.ics.nceph.core.db.dynamoDB.entity;

import java.text.SimpleDateFormat;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;

/**
 * Model class for RELAY_EVENT messages emitted via cerebrum to the subscribing synaptic nodes
 * 
 * @author Chandan Verma
 * @since 5-Aug-2022
 */
@DynamoDBTable(tableName = "Message")
public class ReceivedMessageEntity extends MessageEntity
{ 
	private Integer consumerNodeId;
	
	private Integer consumerPortNumber;
	
	private String appReceptorName;
	
	private Integer appReceptorExecutionTime;
	
	private String appReceptorExecutionErrorMsg;
	
	private Integer appReceptorExecutionAttempts = 0;
	
	private boolean appReceptorFailed;
	
	public ReceivedMessageEntity() 
	{
		super();
	}
	
	@DynamoDBAttribute(attributeName = "consumerNodeId")
	public Integer getConsumerNodeId() 
	{
		return consumerNodeId;
	}

	public void setConsumerNodeId(Integer consumerNodeId) 
	{
		this.consumerNodeId = consumerNodeId;
	}

	@DynamoDBAttribute(attributeName = "consumerPortNumber")
	public Integer getConsumerPortNumber() 
	{
		return consumerPortNumber;
	}

	public void setConsumerPortNumber(Integer consumerPortNumber) 
	{
		this.consumerPortNumber = consumerPortNumber;
	}

	@DynamoDBAttribute(attributeName = "appReceptorName")
	public String getAppReceptorName() 
	{
		return appReceptorName;
	}

	public void setAppReceptorName(String appReceptorName) 
	{
		this.appReceptorName = appReceptorName;
	}

	@DynamoDBAttribute(attributeName = "appReceptorExecutionTime")
	public Integer getAppReceptorExecutionTime() 
	{
		return appReceptorExecutionTime;
	}

	public void setAppReceptorExecutionTime(Integer appReceptorExecutionTime) 
	{
		this.appReceptorExecutionTime = appReceptorExecutionTime;
	}

	@DynamoDBAttribute(attributeName = "appReceptorExecutionAttempts")
	public Integer getAppReceptorExecutionAttempts() 
	{
		return appReceptorExecutionAttempts;
	}

	public void setAppReceptorExecutionAttempts(Integer appReceptorExecutionAttempts) 
	{
		this.appReceptorExecutionAttempts = appReceptorExecutionAttempts;
	}

	@DynamoDBAttribute(attributeName = "appReceptorFailed")
	public boolean getAppReceptorFailed() 
	{
		return appReceptorFailed;
	}

	public void setAppReceptorFailed(boolean appReceptorFailed) 
	{
		this.appReceptorFailed = appReceptorFailed;
	}
	
	@DynamoDBAttribute(attributeName = "appReceptorExecutionErrorMsg")
	public String getAppReceptorExecutionErrorMsg() 
	{
		return appReceptorExecutionErrorMsg;
	}

	public void setAppReceptorExecutionErrorMsg(String appReceptorExecutionErrorMsg) 
	{
		this.appReceptorExecutionErrorMsg = appReceptorExecutionErrorMsg;
	}

	/**
	 * Builder class to build ReceivedMessageEntity object
	 * @author chandan
	 */
	public static class Builder
	{
		private String por;
		
		private String event;
		
		// DynamoDB MessageEntity Table keyPrefix (If message is published then add keyPrefix)
		private String keyPrefix = "R:";

		private final ObjectMapper mapper = new ObjectMapper()
				.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
				.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.setSerializationInclusion(Include.NON_NULL);
		
		public Builder por(String por)
		{
			this.por = por;
			return this;
		}
		
		public Builder event(String event)
		{
			this.event = event;
			return this;
		}
		
		public ReceivedMessageEntity build() throws JsonMappingException, JsonProcessingException
		{
			// Create Publish MessageEntity object using mapper.readValue
			ReceivedMessageEntity receivedMessage = mapper.readValue(por, ReceivedMessageEntity.class);

			// Create Table Primary Key (Partition key and Sort key) 
			Key key = new Key.Builder()
					.partitionKey(keyPrefix + receivedMessage.getConsumerPortNumber())
					.sortKey(receivedMessage.getMessageId())
					.build();
			
			// Set Event Data
			receivedMessage.setEventData(event);
			
			// Set Primary Key
			receivedMessage.setKey(key);
			
			return receivedMessage;
		}
	}
}
