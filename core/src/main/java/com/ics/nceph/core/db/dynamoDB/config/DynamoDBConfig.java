package com.ics.nceph.core.db.dynamoDB.config;

import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.ics.logger.DynamodbLog;
import com.ics.logger.NcephLogger;

/**
 * This is a configuration class for dynamoDB. 
 * It defines a spring container managed bean for {@link AmazonDynamoDB}, which can be injected to other beans as per the requirement.
 *  
 * @author Chandan Verma
 * @since 5-Aug-2022
 */
@Configuration
@EnableDynamoDBRepositories(basePackages = "com.ics.nceph.core.db.dynamoDB.repository")
public class DynamoDBConfig 
{
	
		// AWS DynamoDB Endpoint
		@Value("${amazon.dynamodb.endpoint}")
	    private String amazonDynamoDBEndpoint;
	
		// AWS DynamoDB AccessKey
	    @Value("${amazon.aws.accesskey}")
	    private String amazonAWSAccessKey;
	
	    // AWS DynamoDB Secretkey
	    @Value("${amazon.aws.secretkey}")
	    private String amazonAWSSecretKey;
	    
	    // AWS DynamoDB Region
	    @Value("${amazon.aws.region}")
	    private String region;

	@Bean
	public AmazonDynamoDB amazonDynamoDB() 
	{
		AmazonDynamoDB amazonDynamoDB = null;
		try {
			amazonDynamoDB = AmazonDynamoDBClientBuilder
					.standard()
					.withEndpointConfiguration(new EndpointConfiguration(amazonDynamoDBEndpoint, region))
					.withCredentials(
							new AWSStaticCredentialsProvider(
									new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey)
									)
							)
					.build();
		} catch (Exception e) 
		{
			NcephLogger.DYNAMODB_LOGGER.fatal(new DynamodbLog.Builder()
					.action("DatabaseException")
					.description("Connection Refused: " + e.getMessage())
					.logError(),e);
		}
		return amazonDynamoDB;
	}
}
