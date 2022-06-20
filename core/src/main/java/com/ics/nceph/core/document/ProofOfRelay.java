package com.ics.nceph.core.document;

import java.util.ArrayList;
import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;

/**
 * Data structure to hold the complete information regarding the complex process of message relay.
 * 
 * Scenario 1: The perfect world.
 * 	Step 1: Cerebrum node creates a POD (relayedOn) for the event to relay and saves it in its local file storage & then relay the event (PUBLISH_EVENT). 
 * 			relayedOn is set in the event object (for Synaptic node)
 * 	Step 2: Synaptic node receives it and creates a POD (relayedOn, ackSentOn) in its local storage. And sends back the acknowledgement (NCEPH_EVENT_ACK).
 * 	Step 3: Cerebrum on receiving the acknowledgement message relay acknowledgement received message (ACK_RECEIVED) to synaptic node with networkRecords & acknowledgementRecords
 * 	Step 4:	Synaptic node receives the acknowledgement received message (ACK_RECEIVED) and update the POD (networkRecords & acknowledgementRecords). 
 * 			Then it sends DELETE_POD message back to sender.
 * 	Step 5: Cerebrum deletes the POD from its local storage
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
public class ProofOfRelay extends Document
{
	public static String DOC_PREFIX = "p";
	
	private String messageId;
	
	//Created on
	private long relayedOn;
	
	private Date deliveredOn;
	
	private Date ackReceivedOn;
	
	private EventData event;
	
	private IORecord writeRecord;
	
	private IORecord readRecord;
	
	private IORecord ackWriteRecord;
	
	private IORecord ackReadRecord;
	
	private IORecord threeWayAckWriteRecord;
	
	private IORecord threeWayAckReadRecord;
	
	private NetworkRecord eventNetworkRecord;
	
	private NetworkRecord ackNetworkRecord;
	
	private NetworkRecord threeWayAckNetworkRecord;
	
	private Date DeleteReqTime;
	
	private PorState porState;
	
	private int acknowledgementAttempts = 0;
	
	private int relayAttempts = 0;
	
	private int threeWayAckAttempts = 0;
	
	private int deletePorAttempts = 0;
	
	ProofOfRelay(){
		super.changeLog = new ArrayList<String>();
	}
	
	ProofOfRelay(String messageId, EventData event, long createdOn)
	{
		this.relayedOn = createdOn;
		this.messageId = messageId;
		this.event = event;
		this.porState = PorState.INITIAL;
		super.changeLog = new ArrayList<String>();
		changeLog.add("New");
	}

	public EventData getEvent() {
		return event;
	}

	public IORecord getWriteRecord() {
		return writeRecord;
	}

	public void setWriteRecord(IORecord writeRecord) {
		this.writeRecord = writeRecord;
		outOfSync("WriteRecord");
	}

	public IORecord getReadRecord() {
		return readRecord;
	}
	public int getAcknowledgementAttempts() {
		return acknowledgementAttempts;
	}

	public int getRelayAttempts() {
		return relayAttempts;
	}

	public int getThreeWayAckAttempts() {
		return threeWayAckAttempts;
	}

	public int getDeletePorAttempts() {
		return deletePorAttempts;
	}
	
	public void incrementRelayAttempts() {
		this.relayAttempts++;
		outOfSync("relayAttempts");
	}
	
	public void incrementAcknowledgementAttempts() {
		this.acknowledgementAttempts++;
		outOfSync("acknowledgementAttempts");
	}
	
	public void incrementThreeWayAckAttempts() {
		this.threeWayAckAttempts++;
		outOfSync("threeWayAckAttempts");
	}
	
	public void incrementDeletePorAttempts() {
		this.deletePorAttempts++;
		outOfSync("deletePorAttempts");
	}
	
	public void decrementRelayAttemptsAttempts() {
		this.relayAttempts--;
		outOfSync("publishAttempts");
	}
	
	public void decrementAcknowledgementAttempts() {
		this.acknowledgementAttempts--;
		outOfSync("acknowledgementAttempts");
	}
	
	public void decrementThreeWayAckAttempts() {
		this.threeWayAckAttempts--;
		outOfSync("threeWayAckAttempts");
	}
	
	public void decrementDeletePorAttempts() {
		this.deletePorAttempts--;
		outOfSync("deletePorAttempts");
	}
	
	public void setReadRecord(IORecord readRecord) {
		this.readRecord = readRecord;
		outOfSync("ReadRecord");
	}
	
	public String getMessageId() {
		return messageId;
	}

	public long getRelayedOn() {
		return relayedOn;
	}

	public Date getDeliveredOn() {
		return deliveredOn;
	}

	public void setDeliveredOn(Date deliveredOn) {
		this.deliveredOn = deliveredOn;
		outOfSync("DeliveredOn");
	}

	public Date getAckReceivedOn() {
		return ackReceivedOn;
	}

	public void setAckReceivedOn(Date ackReceivedOn) {
		this.ackReceivedOn = ackReceivedOn;
		outOfSync("AckReceivedOn");
	}

	public NetworkRecord getAckNetworkRecord() {
		return ackNetworkRecord;
	}

	public void setAckNetworkRecord(NetworkRecord ackNetworkRecord) {
		this.ackNetworkRecord = ackNetworkRecord;
		outOfSync("AckNetworkRecord");
	}

	public NetworkRecord getThreeWayAckNetworkRecord() {
		return threeWayAckNetworkRecord;
	}

	public void setThreeWayAckNetworkRecord(NetworkRecord threeWayAckNetworkRecord) {
		this.threeWayAckNetworkRecord = threeWayAckNetworkRecord;
		outOfSync("ThreeWayAckNetworkRecord");
	}

	public NetworkRecord getEventNetworkRecord() {
		return eventNetworkRecord;
	}

	public void setEventNetworkRecord(NetworkRecord eventNetworkRecord) {
		this.eventNetworkRecord = eventNetworkRecord;
		outOfSync("EventNetworkRecord");
	}

	public Date getDeleteReqTime() {
		return DeleteReqTime;
	}

	public void setDeleteReqTime(Date deleteReqTime) {
		DeleteReqTime = deleteReqTime;
		outOfSync("DeleteReqTime");
	}
	
	public IORecord getAckWriteRecord() {
		return ackWriteRecord;
	}

	public void setAckWriteRecord(IORecord ackWriteRecord) {
		this.ackWriteRecord = ackWriteRecord;
		outOfSync("AckWriteRecord");
	}

	public IORecord getAckReadRecord() {
		return ackReadRecord;
	}

	public void setAckReadRecord(IORecord ackReadRecord) {
		this.ackReadRecord = ackReadRecord;
		outOfSync("AckReadRecord");
	}

	public IORecord getThreeWayAckWriteRecord() {
		return threeWayAckWriteRecord;
	}

	public void setThreeWayAckWriteRecord(IORecord threeWayAckWriteRecord) {
		this.threeWayAckWriteRecord = threeWayAckWriteRecord;
		outOfSync("ThreeWayAckWriteRecord");
	}

	public IORecord getThreeWayAckReadRecord() {
		return threeWayAckReadRecord;
	}

	public void setThreeWayAckReadRecord(IORecord threeWayAckReadRecord) {
		this.threeWayAckReadRecord = threeWayAckReadRecord;
		outOfSync("ThreeWayAckReadRecord");
	}
	
	public PorState getPorState() {
		return porState;
	}

	public void setPorState(PorState porState) {
		this.porState = porState;
		outOfSync("PorState");
	}

	public String toString()
	{
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void decrementAttempts()
	{
		if(porState.getState() == 100 || porState.getState() == 200)
			decrementRelayAttemptsAttempts();
		else
			decrementThreeWayAckAttempts();
	}
	
	@Override
	public String localMessageStoreLocation() 
	{
		// TODO Auto-generated method stub
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.relayed_location");
	}
	
	public static class Builder
	{
		private String messageId;
		
		private EventData event;
		
		private long relayedOn;
		
		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
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
			return new ProofOfRelay(messageId, event, this.relayedOn  == 0L? new Date().getTime() : this.relayedOn);
		}
	}
}
