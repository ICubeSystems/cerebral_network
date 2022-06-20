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
	 * Default constructor used by ObjectMapper to serialize/ deserialize this object
	 */
	public AcknowledgementData() {}
	
	private AcknowledgementData(IORecord readRecord, NetworkRecord ackNetworkRecord, NetworkRecord eventNetworkRecord)
	{
		this.readRecord = readRecord;
		this.ackNetworkRecord = ackNetworkRecord;
		this.eventNetworkRecord = eventNetworkRecord;
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


	public static class Builder
	{
		private IORecord readRecord;

		private NetworkRecord ackNetworkRecord;
		
		private NetworkRecord eventNetworkRecord;
		
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
		
		public Builder eventNetworkRecord(NetworkRecord eventNetworkRecord)
		{
			this.eventNetworkRecord = eventNetworkRecord;
			return this;
		}
		
		public AcknowledgementData build() 
		{
			return new AcknowledgementData(readRecord, ackNetworkRecord, eventNetworkRecord);
		}
	}
}
