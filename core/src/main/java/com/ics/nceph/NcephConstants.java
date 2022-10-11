package com.ics.nceph;

import com.ics.nceph.core.Configuration;

public class NcephConstants 
{
	/**
	 * Length of the message header
	 */
	public static int MESSAGE_HEADER_LENGTH = 24;
	
	/**
	 * Size of the buffer used in MessageReader to hold plain text
	 */
	public static int READER_BUFFER_SIZE = 4096*16;
	
	/**
	 * TLS communication over the transport layer
	 */
	public static boolean TLS_MODE = true;
	
	/**
	 * Interval of execution for monitor thread in seconds
	 */
	public static int MONITOR_INTERVAL = 60;
	
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static int QUEUED_FROM_CONNECTOR = 100;
	
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static int QUEUED_FROM_EMITTER = 200;
	
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static int QUEUED_FROM_RECEPTOR = 300;
	
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static int QUEUED_FROM_MONITOR = 400;
	
	/**
	 * Save message in DynamoDB or local stores (True, False)
	 */
	public static final boolean saveInDB = Boolean.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("messages.saveindb"));
	

	/**
	 * {@link ProofOfPublish} & {@link ProofOfRelay} objects for the messages in transit can be stored in a local datastore or in an external database like DynamoDB.
	 * <ul>
	 * 	<li><b>100:</b> Local storage as files in a pre specified directory. <i>[Default]</i></li>
	 * 	<li><b>200:</b> External database like DynamoDB. <i>[Note: {@link NcephConstants#MESSAGE_PERSISTANCE MESSAGE_PERSISTANCE} should be true for TRANSIT_MESSAGE_STORAGE to be set to external database (200)]</i></li>
	 * </ul>
	 */
	public static final int TRANSIT_MESSAGE_STORAGE = Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("messages.transit"));

}
