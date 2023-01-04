package com.ics.logger.S3Backup;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.ics.nceph.core.Configuration;

/**
 * Singleton class to get object of configured {@link AmazonS3} instance.
 * @author Anshul
 * @version 1.0
 * @since Dec 12, 2022
 */
public class S3Instance {
	public static AmazonS3 s3;
	
	public static AmazonS3 getInstance() {
		if(s3 == null) {
			// Create AmazonS3 object
			s3 = AmazonS3ClientBuilder
					  .standard()
					  .withCredentials(
							  new AWSStaticCredentialsProvider(
									  new BasicAWSCredentials(		// Set credentials
											  Configuration.APPLICATION_PROPERTIES.getConfig("AWS_ACCESS_KEY"), 		// set aws access key 
											  Configuration.APPLICATION_PROPERTIES.getConfig("AWS_SECRET_KEY")		// set aws secret key
											  )
									  )
							  )
					  .withRegion(Regions.US_EAST_1)	// set region
					  .build();
			}
		return s3;
	}
	
}
