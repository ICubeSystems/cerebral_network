package com.ics.nceph;

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
	public static boolean saveInDB = true;
	
}
