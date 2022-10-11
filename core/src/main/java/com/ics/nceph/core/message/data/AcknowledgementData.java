package com.ics.nceph.core.message.data;

import java.io.Serializable;

import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 30-Mar-2022
 */
public class AcknowledgementData extends MessageData implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Time taken in reading the message (PUBLISH_EVENT) for which this ACK is being sent
	 */
	IORecord readRecord;
	
	/**
	 * Network latency time for this ACK. Start is set on the sending side, ackNetworkRecord is completed once it is received on the receiving side.
	 */
	NetworkRecord ackNetworkRecord;
	
	/**
	 * Network latency time for this event
	 */
	NetworkRecord eventNetworkRecord;
	
	/**
	 * Name of the application receptor according to event type
	 */
	String appReceptorName;
	
	/**
	 * In case of application receptor execution failed, it contains error message of ApplicationReceptorFailedException 
	 */
	String appReceptorExecutionErrorMsg;
	
	/**
	 * Execution time of executing application receptor
	 */
	long appReceptorExecutionTime;
	
	/**
	 * status or application receptor execution
	 */
	boolean appReceptorFailed;
	
	/**
	 * 
	 */
	int nodeId;
	
	/**
	 * Default constructor used by ObjectMapper to serialize/ deserialize this object
	 */
	public AcknowledgementData() {}
	

	private AcknowledgementData(IORecord readRecord, NetworkRecord ackNetworkRecord, NetworkRecord eventNetworkRecord, String appReceptorName, String appReceptorExecutionErrorMsg, long appReceptorExecutionTime, boolean appReceptorFailed, int nodeId)

	{
		this.readRecord = readRecord;
		this.ackNetworkRecord = ackNetworkRecord;
		this.eventNetworkRecord = eventNetworkRecord;
		this.appReceptorName = appReceptorName;
		this.appReceptorExecutionErrorMsg = appReceptorExecutionErrorMsg;
		this.appReceptorExecutionTime = appReceptorExecutionTime;
		this.appReceptorFailed = appReceptorFailed;
		this.nodeId = nodeId;
	}
	
	public IORecord getReadRecord() {
		return readRecord;
	}
	
	public NetworkRecord getAckNetworkRecord() {
		return ackNetworkRecord;
	}
	
	public NetworkRecord getEventNetworkRecord() {
		return eventNetworkRecord;
	}

	public String getAppReceptorName() {
		return appReceptorName;
	}

	public String getAppReceptorExecutionErrorMsg() {	
		return appReceptorExecutionErrorMsg;
	}

	public long getAppReceptorExecutionTime() {
		return appReceptorExecutionTime;
	}
	
	public boolean isAppReceptorFailed()
	{
		return appReceptorFailed;
	}

	public int getNodeId() 
	{
		return nodeId;
	}

	public static class Builder
	{
		private IORecord readRecord;

		private NetworkRecord ackNetworkRecord;
		
		private NetworkRecord eventNetworkRecord;
		
		private String appReceptorName;
		
		private String appReceptorExecutionErrorMsg;
		
		private long appReceptorExecutionTime;
		
		boolean appReceptorFailed;

		private int nodeId;
		
		public Builder readRecord(IORecord readRecord)
		{
			this.readRecord = readRecord;
			return this;
		}
		
		public Builder ackNetworkRecord(NetworkRecord ackNetworkRecord)
		{
			this.ackNetworkRecord = ackNetworkRecord;
			return this;
		}
		
		public Builder nodeId(int nodeId)
		{
			this.nodeId = nodeId;
			return this;
		}
		
		public Builder eventNetworkRecord(NetworkRecord eventNetworkRecord)
		{
			this.eventNetworkRecord = eventNetworkRecord;
			return this;
		}
		
		public Builder AppReceptorName(String appReceptorName)
		{
			this.appReceptorName = appReceptorName;
			return this;
		}
		
		public Builder AppReceptorExecutionErrorMsg(String appReceptorExecutionErrorMsg)
		{
			this.appReceptorExecutionErrorMsg = appReceptorExecutionErrorMsg;
			return this;
		}
		
		public Builder AppReceptorExecutionTime(long appReceptorExecutionTime)
		{
			this.appReceptorExecutionTime = appReceptorExecutionTime;
			return this;
		}
		
		public Builder appReceptorFailed(boolean appReceptorFailed)
		{
			this.appReceptorFailed = appReceptorFailed;
			return this;
		}
		
		public AcknowledgementData build() 
		{
			return new AcknowledgementData(readRecord, ackNetworkRecord, eventNetworkRecord, appReceptorName, appReceptorExecutionErrorMsg, appReceptorExecutionTime, appReceptorFailed, nodeId);
		}
	}
}
