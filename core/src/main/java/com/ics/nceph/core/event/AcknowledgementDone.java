package com.ics.nceph.core.event;

import java.io.Serializable;

import com.ics.nceph.core.message.MessageData;
import com.ics.nceph.core.message.NetworkRecord;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 30-Mar-2022
 */
public class AcknowledgementDone extends MessageData implements Serializable
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
	 * Default constructor used by ObjectMapper to serialize/ deserialize this object
	 */
	public AcknowledgementDone() {}
	
	private AcknowledgementDone(NetworkRecord threeWayAckNetworkRecord)
	{
		this.threeWayAckNetworkRecord = threeWayAckNetworkRecord;
	}	
	
	public NetworkRecord getThreeWayAckNetworkRecord() {
		return threeWayAckNetworkRecord;
	}

	public static class Builder
	{
		private NetworkRecord threeWayAckNetworkRecord;
		
		
		public Builder threeWayAckNetworkRecord(NetworkRecord threeWayAckNetworkRecord)
		{
			this.threeWayAckNetworkRecord = threeWayAckNetworkRecord;
			return this;
		}
		
		
		public AcknowledgementDone build() 
		{
			return new AcknowledgementDone(threeWayAckNetworkRecord);
		}
	}
}
