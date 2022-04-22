package com.ics.nceph.core.event;

import java.io.Serializable;

import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.MessageData;
import com.ics.nceph.core.message.NetworkRecord;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 30-Mar-2022
 */
public class ThreeWayAcknowledgement extends MessageData implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Network latency time for this ACK. Start is set on the sending side, threeWayAckNetworkRecord is completed once it is received on the receiving side.
	 */
	NetworkRecord threeWayAckNetworkRecord;
	
	/**
	 * Writing time of event from publisher() to receiver; 
	 */
	IORecord writeRecord;
	
	/**
	 * Writing time of event from publisher() to receiver; 
	 */
	NetworkRecord ackNetworkRecord;
	
	/**
	 * Default constructor used by ObjectMapper to serialize/ deserialize this object
	 */
	public ThreeWayAcknowledgement() {}
	
	private ThreeWayAcknowledgement(NetworkRecord threeWayAckNetworkRecord, IORecord writeRecord, NetworkRecord ackNetworkRecord)
	{
		this.threeWayAckNetworkRecord = threeWayAckNetworkRecord;
		this.writeRecord = writeRecord;
		this.ackNetworkRecord = ackNetworkRecord;
	}	
	
	public NetworkRecord getThreeWayAckNetworkRecord() {
		return threeWayAckNetworkRecord;
	}

	public IORecord getWriteRecord() {
		return writeRecord;
	}

	public NetworkRecord getAckNetworkRecord() {
		return ackNetworkRecord;
	}
	
	public String toString() {
		return "{3-WayAckNetworkRecordStart:" + threeWayAckNetworkRecord.getStart() + 
				", 3-WayAckNetworkRecordEnd:" + threeWayAckNetworkRecord.getEnd() +
				", writeRecordStart:" + writeRecord.getStart() +
				", writeRecordEnd:" + writeRecord.getEnd() +
				", ackNetworkRecordStart:" + ackNetworkRecord.getStart() +
				", ackNetworkRecordEnd:" + ackNetworkRecord.getEnd() +
				'}';
	}

	public static class Builder
	{
		private NetworkRecord threeWayAckNetworkRecord;
		
		private NetworkRecord ackNetworkRecord;
		
		private IORecord writeRecord;
		
		public Builder threeWayAckNetworkRecord(NetworkRecord threeWayAckNetworkRecord)
		{
			this.threeWayAckNetworkRecord = threeWayAckNetworkRecord;
			return this;
		}
		
		public Builder writeRecord(IORecord writeRecord)
		{
			this.writeRecord = writeRecord;
			return this;
		}
		public Builder ackNetworkRecord(NetworkRecord ackNetworkRecord)
		{
			this.ackNetworkRecord = ackNetworkRecord;
			return this;
		}
		
		public ThreeWayAcknowledgement build() 
		{
			return new ThreeWayAcknowledgement(threeWayAckNetworkRecord, writeRecord, ackNetworkRecord);
		}
	}
}
