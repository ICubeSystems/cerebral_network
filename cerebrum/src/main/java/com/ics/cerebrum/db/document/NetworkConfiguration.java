package com.ics.cerebrum.db.document;

import java.util.List;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.ics.nceph.core.db.document.Document;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.Writer;

import lombok.Getter;
import lombok.Setter;

/**
 * Model class for application configurations
 * 
 * @author Anshul
 * @version 1.0
 * @since Sep 10, 2022
 */
@Getter
@Setter
@DynamoDBTable(tableName = "message_uat")
public class NetworkConfiguration extends Document
{
	/**
	 * Port number assigned to an application in the cerebrum. Every application in the network has a unique port number. This can also be thought of as a unique application id of the application in the network.
	 */
	private Integer port;
	
	/**
	 * Name of the application. This is used for reporting purposes only.
	 */
	private String name;
	
	/**
	 * Configurations for the {@link Reader read worker} thread pool for the application
	 */
	private WorkerPool readerPool;
	
	/**
	 * Configurations for the {@link Writer write worker} thread pool for the application
	 */
	private WorkerPool writerPool;
	
	/**
	 * List of events subscribed by the application 
	 */
	private List<Subscription> subscriptions;
}
