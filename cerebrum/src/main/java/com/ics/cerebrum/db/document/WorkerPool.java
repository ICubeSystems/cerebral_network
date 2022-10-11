package com.ics.cerebrum.db.document;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 15-Feb-2022
 */
@Getter
@Setter
@DynamoDBDocument
public class WorkerPool 
{
	/**
	 * Initial pool size for the worker thread pool
	 */
	private int corePoolSize;

	/**
	 * Maximum number of worker threads allowed in the worker thread pool
	 */
	private int maximumPoolSize;
	
	/**
	 * Maximum time that the idle threads will wait for new tasks before terminating
	 */
	private int keepAliveTime;
}
