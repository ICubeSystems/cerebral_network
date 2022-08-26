package com.ics.nceph.core.document;

import java.util.ArrayList;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.event.EventData;

/**
 * Data structure to hold the complete information regarding the complex process of message relay.<br>
 * 
 * <b>Scenario 1:</b> The perfect world.<br>
 * 	<b>Step 1:</b> Cerebrum node creates a POD (relayedOn) for the event to relay and saves it in its local file storage & then relay the event (PUBLISH_EVENT). 
 * 			relayedOn is set in the event object (for Synaptic node)<br>
 * 	<b>Step 2:</b> Synaptic node receives it and creates a POD (relayedOn, ackSentOn) in its local storage. And sends back the acknowledgement (NCEPH_EVENT_ACK).<br>
 * 	<b>Step 3:</b> Cerebrum on receiving the acknowledgement message relay acknowledgement received message (ACK_RECEIVED) to synaptic node with networkRecords & acknowledgementRecords<br>
 * 	<b>Step 4:</b>	Synaptic node receives the acknowledgement received message (ACK_RECEIVED) and update the POD (networkRecords & acknowledgementRecords). <br>
 * 			Then it sends DELETE_POD message back to sender.<br>
 * 	<b>Step 5:</b> Cerebrum deletes the POD from its local storage<br>
 * 
 * 							Cerebral Node																	Synaptic Node
 * 
 * 													  				 	  RELAY_EVENT
 * 	1)					EventData 1 (EventData relay) 		  			--------------------------------> 		EventData Message Received
 * 						Create POD (relayedOn, writeRecord)		
 * 
 * 																		  NCEPH_EVENT_ACK
 * 	2)					Ack received (for EventData 1)	  			<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Update POD (ackNetworkRecord)													Create POD (relayedOn, readRecord, ackNetworkRecord.start)
 * 	
 * 																		   ACK_RECEIVED
 * 	3)		Acknowledge the receipt of Ack (for EventData 1)		-------------------------------->   	ACK_RECEIVED Message Received
 * 			Update POD (3wayAckNetworkRecord.start)														Update POD (writeRecord, ackNetworkRecord, 3wayAckNetworkRecord)
 * 
 * 																		   DELETE_POD
 * 	4)			Delete the POD from the local storage			<--------------------------------   	Send DELETE_POD message back to sender
 * 																										Update POD (DeleteReqTime)
 * 																										
 * 
 * ===========================================================================================================================================
 *
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 08-Mar-2022
 */
public class ProofOfRelay extends ProofOfDelivery
{
	public static String DOC_PREFIX = "p";
	
	/**
	 * Message Receiver NodeId {@link consumerNodeId}
	 */
	private Integer consumerNodeId;
	
	/**
	 * Message Receiver Port Number {@link consumerPortNumber}
	 */
	private Integer consumerPortNumber;
	
	/**
	 * 
	 */
	private String appReceptorName;
	
	/**
	 * 
	 */
	private long appReceptorExecutionTime;
	
	/**
	 * 
	 */
	private String appReceptorExecutionErrorMsg;
	
	/**
	 * 
	 */
	private int appReceptorExecutionAttempts = 0;
	
	/**
	 * 
	 */
	private boolean appReceptorFailed;
	
	ProofOfRelay() {
		changeLog = new ArrayList<String>();
	}
	
	ProofOfRelay(String messageId, EventData event, long createdOn, Integer nodeId)
	{
		changeLog = new ArrayList<String>();
		this.createdOn = createdOn;
		setMessageId(messageId);
		setEvent(event);
		setMessageDeliveryState(MessageDeliveryState.INITIAL);
		this.consumerNodeId = nodeId;
		changeLog.add("New");
	}
	
	public Integer getConsumerPortNumber() {
		return consumerPortNumber;
	}

	public void setConsumerPortNumber(Integer consumerPortNumber) 
	{
		this.consumerPortNumber = consumerPortNumber;
	}

	public String getAppReceptorName() {
		return appReceptorName;
	}

	public void setAppReceptorName(String appReceptorName) {
		this.appReceptorName = appReceptorName;
	}

	public long getAppReceptorExecutionTime() {
		return appReceptorExecutionTime;
	}

	public void setAppReceptorExecutionTime(long appReceptorExecutionTime) {
		this.appReceptorExecutionTime = appReceptorExecutionTime;
	}

	public String getAppReceptorExecutionErrorMsg() {
		return appReceptorExecutionErrorMsg;
	}

	public void setAppReceptorExecutionErrorMsg(String appReceptorExecutionErrorMsg) {
		this.appReceptorExecutionErrorMsg = appReceptorExecutionErrorMsg;
	}

	public int getAppReceptorExecutionAttempts() {
		return appReceptorExecutionAttempts;
	}

	public void incrementAppReceptorExecutionAttempts() {
		this.appReceptorExecutionAttempts++;
	}
	
	public boolean isAppReceptorFailed() 
	{
		return appReceptorFailed;
	}

	public void setAppReceptorFailed(boolean appReceptorFailed)
	{
		this.appReceptorFailed = appReceptorFailed;
	}

	public Integer getConsumerNodeId() 
	{
		return consumerNodeId;
	}

	public void setConsumerNodeId(Integer nodeId) 
	{
		this.consumerNodeId = nodeId;
		outOfSync("consumerNodeId");
	}

	public String toString()
	{
		ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(Include.NON_NULL);
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void decrementAttempts()
	{
		if(getMessageDeliveryState().getState() == 100 || getMessageDeliveryState().getState() == 200)
			decrementEventMessageAttempts();
		else
			decrementThreeWayAckMessageAttempts();
	}
	
	@Override
	public String localMessageStoreLocation() 
	{
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.relayed_location");
	}
	
	@Override
	public String getName() {
		return "POR";
	}
	
	public static class Builder
	{
		private String messageId;
		
		private EventData event;
		
		private int nodeId;
		
		private long relayedOn;
		
		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
			return this;
		}
		
		public Builder nodeId(int nodeId)
		{
			this.nodeId = nodeId;
			return this;
		}
		
		public Builder event(EventData event)
		{
			this.event = event;
			return this;
		}

		public Builder relayedOn(long relayedOn)
		{
			this.relayedOn = relayedOn;
			return this;
		}
		
		public ProofOfRelay build()
		{
			return new ProofOfRelay(messageId, event, this.relayedOn  == 0L? new Date().getTime() : this.relayedOn, nodeId);
		}
	}
}
