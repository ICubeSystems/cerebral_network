package com.ics.cerebrum.db.config;

import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.ics.env.Environment;

/**
 * This is a configuration class for dynamoDB. 
 * It defines a spring container managed bean for {@link AmazonDynamoDB}, which can be injected to other beans as per the requirement.
 *  
 * @author Chandan Verma
 * @since 5-Aug-2022
 */
@Configuration
@EnableDynamoDBRepositories(basePackages = {"com.ics.nceph.core.db.repository", "com.ics.cerebrum.db.repository"})
public class DynamoDBConfig 
{
	@Bean
	public AmazonDynamoDB amazonDynamoDB() 
	{
		return AmazonDynamoDBClientBuilder
				.standard()
				.withCredentials(new ProfileCredentialsProvider(!Environment.isProd() ? AWSProfiles.UAT.getProfile() : AWSProfiles.PROD.getProfile()))
				.withEndpointConfiguration(new EndpointConfiguration("dynamodb.us-east-1.amazonaws.com", "us-east-1"))
				.build();
	}
}
