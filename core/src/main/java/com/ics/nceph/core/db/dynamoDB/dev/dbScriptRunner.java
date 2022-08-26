package com.ics.nceph.core.db.dynamoDB.dev;

import java.util.ArrayList;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.GlobalSecondaryIndex;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.Projection;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;

/**
 * This class is used to build a fresh dynamoDB instance on the local development environment. 
 * <b>This is not required for any other environments.</b>
 * 
 * @author Chandan Verma
 * @since 1-Aug-2022
 */
public class dbScriptRunner 
{
	static public String tableName = "Message";

	public static AmazonDynamoDB amazonDynamoDB() 
	{
		return AmazonDynamoDBClientBuilder
				.standard()
				.withEndpointConfiguration(new EndpointConfiguration("http://localhost:8000", "us-east-1"))
				.withCredentials(amazonAWSCredentials())
				.build();
	}

	public static AWSCredentialsProvider amazonAWSCredentials() {
		return new AWSStaticCredentialsProvider(new BasicAWSCredentials("ncephNetwork", "nceph"));
	}

	static DynamoDB dynamodb = new DynamoDB(amazonDynamoDB());  

	public static void main(String[] args) throws Exception 
	{
		deleteTable();
		createTable();
	}

	public static void createTable() 
	{
		// Attribute definitions
		ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();

		// add Attribute partitionKey
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("partitionKey").withAttributeType("S"));
		// add Attribute sortKey
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("sortKey").withAttributeType("S"));
		// add Attribute messageId
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("messageId").withAttributeType("S"));
		// add Attribute portNumber
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("producerPortNumber").withAttributeType("N"));
		// add Attribute eventType
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("eventType").withAttributeType("N"));
		// add Attribute eventId
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("eventId").withAttributeType("N"));
		// add Attribute nodeId
		attributeDefinitions.add(new AttributeDefinition().withAttributeName("producerNodeId").withAttributeType("N"));

		// Key schema for table
		ArrayList<KeySchemaElement> tableKeySchema = new ArrayList<KeySchemaElement>();

		// add tableKeySchema partitionKey (HASH)
		tableKeySchema.add(new KeySchemaElement().withAttributeName("partitionKey").withKeyType(KeyType.HASH)); // Partition key
		// add tableKeySchema sortKey (RANGE)
		tableKeySchema.add(new KeySchemaElement().withAttributeName("sortKey").withKeyType(KeyType.RANGE));  // Sort key

		// Initial provisioned throughput settings for the indexes
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
				// Table and Index Read Capacity Units
				.withReadCapacityUnits(1L)
				// Table and Index Write Capacity Units
				.withWriteCapacityUnits(1L);

		// Global Secondary Index EventTypeIndex
		GlobalSecondaryIndex eventTypeIndex = new GlobalSecondaryIndex().withIndexName("EventTypeIndex") // Index name EventTypeIndex
				// Provisioned throughput read write capacity units
				.withProvisionedThroughput(provisionedThroughput)
				// add HASH and RANGE Key in the Index KeySchema
				.withKeySchema(new KeySchemaElement().withAttributeName("eventType").withKeyType(KeyType.HASH),
						new KeySchemaElement().withAttributeName("messageId").withKeyType(KeyType.RANGE))   
				// Set projection  (add all items in index)
				.withProjection(new Projection().withProjectionType("ALL"));

		// Global Secondary Index EventIdIndex
		GlobalSecondaryIndex eventIdIndex = new GlobalSecondaryIndex().withIndexName("EventIdIndex") // Index name EventIdIndex
				// Provisioned throughput read write capacity units
				.withProvisionedThroughput(provisionedThroughput)
				// add HASH and RANGE Key in the Index KeySchema
				.withKeySchema(new KeySchemaElement().withAttributeName("eventId").withKeyType(KeyType.HASH),
						new KeySchemaElement().withAttributeName("eventType").withKeyType(KeyType.RANGE))     
				// Set projection  (add all items in index)
				.withProjection(new Projection().withProjectionType("ALL"));

		// Global Secondary Index ProducerNodeIdIndex
		GlobalSecondaryIndex producerNodeIdIndex = new GlobalSecondaryIndex().withIndexName("ProducerNodeIdIndex") // Index name ProducerNodeIdIndex
				// Provisioned throughput read write capacity units
				.withProvisionedThroughput(provisionedThroughput)
				// add HASH and RANGE Key in the Index KeySchema
				.withKeySchema(new KeySchemaElement().withAttributeName("producerNodeId").withKeyType(KeyType.HASH),
						new KeySchemaElement().withAttributeName("messageId").withKeyType(KeyType.RANGE))  
				// Set projection  (add all items in index)
				.withProjection(new Projection().withProjectionType("ALL"));

		// Global Secondary Index MessageIdIndex
		GlobalSecondaryIndex messageIdIndex = new GlobalSecondaryIndex().withIndexName("MessageIdIndex") // Index name MessageIdIndex
				// Provisioned throughput read write capacity units
				.withProvisionedThroughput(provisionedThroughput)
				// add HASH and RANGE Key in the Index KeySchema
				.withKeySchema(new KeySchemaElement().withAttributeName("messageId").withKeyType(KeyType.HASH),
						new KeySchemaElement().withAttributeName("producerNodeId").withKeyType(KeyType.RANGE))      
				// Set projection  (add all items in index)
				.withProjection(new Projection().withProjectionType("ALL"));
		
		// Global Secondary Index ProducerPortNumberIndex
		GlobalSecondaryIndex producerPortNumberIndex = new GlobalSecondaryIndex().withIndexName("ProducerPortNumberIndex") // Index name MessageIdIndex
				// Provisioned throughput read write capacity units
				.withProvisionedThroughput(provisionedThroughput)
				// add HASH and RANGE Key in the Index KeySchema
				.withKeySchema(new KeySchemaElement().withAttributeName("producerPortNumber").withKeyType(KeyType.HASH),
						new KeySchemaElement().withAttributeName("producerNodeId").withKeyType(KeyType.RANGE))      
				// Set projection  (add all items in index)
				.withProjection(new Projection().withProjectionType("ALL"));

		// TableRequest
		CreateTableRequest createTableRequest = new CreateTableRequest()
				// Table Name
				.withTableName(tableName)
				// Provisioned throughput table read write capacity units
				.withProvisionedThroughput(provisionedThroughput)
				// Attribute Definitions
				.withAttributeDefinitions(attributeDefinitions)
				// Table key schema
				.withKeySchema(tableKeySchema)
				// Global secondary indexs
				.withGlobalSecondaryIndexes(eventTypeIndex, eventIdIndex, producerNodeIdIndex, messageIdIndex, producerPortNumberIndex);


		System.out.println("Creating table " + tableName + "...");
		
		// Create table in DynamoDB
		dynamodb.createTable(createTableRequest);

		// Wait for table to become active
		System.out.println("Waiting for " + tableName + " to become ACTIVE...");
		try {
			Table table = dynamodb.getTable(tableName);
			table.waitForActive();
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteTable() {
		Table table = dynamodb.getTable(tableName);
		try { 
			System.out.println("Performing table delete, wait..."); 
			table.delete(); 
			table.waitForDelete(); 
			System.out.print("Table successfully deleted.");  
		} catch (Exception e) { 
			System.err.println("Cannot perform table delete: "); 
			System.err.println(e.getMessage()); 
		}
	}
}
