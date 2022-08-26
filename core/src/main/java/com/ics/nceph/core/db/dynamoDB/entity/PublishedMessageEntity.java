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
 * Model class for PUBLISH_EVENT messages emitted via synapse on the network
 * 
 * @author Chandan Verma
 * @since 5-Aug-2022
 * 
 */
@DynamoDBTable(tableName = "Message")
public class PublishedMessageEntity extends MessageEntity
{ 	
	private Integer subscriberCount;
	
	private Integer relayCount;
	
	public PublishedMessageEntity() {
		super();
	}
	
	@DynamoDBAttribute(attributeName = "subscriberCount")
	public Integer getSubscriberCount() 
	{
		return subscriberCount;
	}

	public void setSubscriberCount(Integer subscriberCount) 
	{
		this.subscriberCount = subscriberCount;
	}
	
	@DynamoDBAttribute(attributeName = "relayCount")
	public Integer getRelayCount() 
	{
		return relayCount;
	}

	public void setRelayCount(Integer relayCount) 
	{
		this.relayCount = relayCount;
	}

	/**
	 * Builder class to build PublishedMessageEntity object
	 * @author chandan
	 */
	public static class Builder
	{
		private String pod;
		
		// DynamoDB MessageEntity Table keyPrefix (If message is published then add keyPrefix)
		private String keyPrefix = "P:";

		// Mapper
		private final ObjectMapper mapper = new ObjectMapper()
				.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
				.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
				.setSerializationInclusion(Include.NON_NULL);
		
		public Builder pod(String pod)
		{
			this.pod = pod;
			return this;
		}
		
		public PublishedMessageEntity build() throws JsonMappingException, JsonProcessingException
		{
			// Create Publish MessageEntity object using mapper.readValue
			PublishedMessageEntity publishMessage = mapper.readValue(pod, PublishedMessageEntity.class);
			
			// Create Table Primary Key (Partition key and Sort key) 
			Key key = new Key.Builder()
					.partitionKey(keyPrefix + publishMessage.getProducerPortNumber())
					.sortKey(publishMessage.getMessageId())
					.build();
			
			// Set Primary Key
			publishMessage.setKey(key);
			
			return publishMessage;
		}
	}
}
