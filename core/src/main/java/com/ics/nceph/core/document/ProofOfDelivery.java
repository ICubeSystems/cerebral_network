package com.ics.nceph.core.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;

/**
 * Data structure to hold the complete information regarding the complex process of message delivery.
 * 
 * Scenario 1: The perfect world.
 * 	Step 1: Synaptic node creates a this (createdOn) for the event to emit and saves it in its local file storage & then emits the event (PUBLISH_EVENT). 
 * 			CreatedOn is set in the event object (for Cerebrum)
 * 	Step 2: Cerebrum receives it and creates a this (createdOn, ackSentOn) in its local storage. And sends back the acknowledgement (NCEPH_EVENT_ACK).
 * 	Step 3: Synaptic node on receiving the acknowledgement message sends acknowledgement received message (ACK_RECEIVED) to cerebrum with networkRecords & ackNetworkRecord
 * 	Step 4:	Cerebrum receives the acknowledgement received message (ACK_RECEIVED) and update the this (networkRecords & ackNetworkRecord). 
 * 			Then it sends DELETE_this message back to sender.
 * 	Step 5: Synaptic node deletes the this from its local storage
 * 
 * 							Synaptic Node																	Cerebral Node
 * 
 * 													  				 	  PUBLISH_EVENT
 * 	1)					EventData 1 (Gift created) 		  			--------------------------------> 		EventData Message Received
 * 						Create this (createdOn, writeRecord)		
 * 
 * 																		  NCEPH_EVENT_ACK
 * 	2)					Ack received (for EventData 1)	  			<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Update this (ackNetworkRecord)													Create this (createdOn, readRecord, ackNetworkRecord.start)
 * 	
 * 																		   ACK_RECEIVED
 * 	3)		Acknowledge the receipt of Ack (for EventData 1)		-------------------------------->   	ACK_RECEIVED Message Received
 * 			Update this (3wayAckNetworkRecord.start)														Update this (writeRecord, ackNetworkRecord, 3wayAckNetworkRecord)
 * 
 * 																		   DELETE_this
 * 	4)			Delete the this from the local storage			<--------------------------------   	Send DELETE_this message back to sender
 * 																										Update this (DeleteReqTime)
 * 																										
 * 
 * ===========================================================================================================================================
 * 
 * Scenario 2: Synaptic node does not receive acknowledgement message (NCEPH_EVENT_ACK) from cerebrum. This may be due to any of the reasons like crash or network failure.
 * 	Step 1: Synaptic node creates a this (createdOn) for the event to emit and saves it in its local file storage & then emits the event (PUBLISH_EVENT). 
 * 			CreatedOn is set in the event object (for Cerebrum)
 * 	Step 2: Cerebrum receives it and creates a this (createdOn, ackSentOn) in its local storage. And sends back the acknowledgement (NCEPH_EVENT_ACK).
 * 	Step 3: Synaptic node does not receive the acknowledgement message (NCEPH_EVENT_ACK) from cerebrum. Either it crashes or there is a network failure
 * 
 * Scenario 2.1 (System crash):  Synaptic node reboots after the crash
 * 	Step 4: During the bootstraping process it checks for thiss on the local storage. If there are any thiss then they are queued again to be resent.
 * 
 *							Synaptic Node																	Cerebral Node
 * 
 * 													  				 	  PUBLISH_EVENT
 * 	1)					EventData 1 (Gift created) 		  			--------------------------------> 		Message Received
 * 						Create this (createdOn)		
 * 
 * 																		 NCEPH_EVENT_ACK
 * 	2)					System Crash (Hardware/ software)		<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 																										Create this (createdOn, ackSentOn, ackAttempt = 1)
 * 
 * 																		  PUBLISH_EVENT
 * 	3)					System Reboot							--------------------------------> 		Message Received		  
 * 						Resend messages with undeleted thiss 	
 * 
 *   																   NCEPH_EVENT_ACK_AGAIN
 * 	4)					Ack received (for EventData 1) 				<-------------------------------- 		Re-Acknowledge the receipt of the PUBLISH_EVENT message to the sender		  
 * 																										Update this (ackSentOn, ackAttempt++)
 * 
 *  																	   ACK_RECEIVED
 * 	5)		Acknowledge the receipt of Ack (for EventData 1)		-------------------------------->   	ACK_RECEIVED Message Received
 * 																										Update this (networkRecords & ackNetworkRecord)
 * 
 * 																		   DELETE_this
 * 	6)			Delete the this from the local storage			<--------------------------------   	Send DELETE_this message back to sender
 * 																										
 *
 * Scenario 2.2 (Network failure):  The cerebral monitor thread periodically checks for the thiss on the local storage
 * 	Step 4: If the thiss are not deleted for a pre-defined time then they are queued again to be resent.   
 * 
 * 							Synaptic Node																	Cerebral Node
 * 
 * 													  				 	  PUBLISH_EVENT
 * 	1)					EventData 1 (Gift created) 		  			--------------------------------> 		Message Received
 * 						Create this (createdOn)		
 * try {
			 FileChannel channel = new RandomAccessFile(file, lockingMode).getChannel();
			 // Acquire an exclusive lock on this channel's file (blocks until lock can be retrieved)
			 lock = channel.lock();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 * 																		NCEPH_EVENT_ACK
 * 	2)					Network timeout (failure/ congestion)	<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Connection closed																Create this (createdOn, ackSentOn, ackAttempt = 1)
 * 
 * 																		  PUBLISH_EVENT
 * 	3)					System Reboot							--------------------------------> 		Message Received		  
 * 						Resend messages with undeleted thiss 	
 * 
 *   																   NCEPH_EVENT_ACK_AGAIN
 * 	4)					Acknowledge the receipt of Ack 			<-------------------------------- 		Re-Acknowledge the receipt of the PUBLISH_EVENT message to the sender		  
 * 																										Update this (ackSentOn, ackAttempt++)
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *							Synaptic Node																	Cerebral Node
 * 
 * 													  				 	   PUBLISH_EVENT
 * 	1)					EventData 1 (Gift created) 		  			--------------------------------> 		Message Received
 * 						Create this (createdOn)		
 * 
 * 																		 NCEPH_EVENT_ACK
 * 	2.1)				Ack received (for EventData 1)	  			<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Delete this 																		Create this (createdOn, ackSentOn)
 * 	
 * 																	   NCEPH_EVENT_ACK_AGAIN
 * 	2.2)				Ack received (for EventData 1)	  			<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Delete this 																		Create this (createdOn, ackSentOn)
 * 																										In case when the PUBLISH_EVENT message is sent again due to 
 * 																										fail over of the synaptic node. Or connector monitoring thread finds out 
 * 																										ACK_RECEIVED message is not received for more than a specified time period.
 * 																										Or RelayTimeoutException is thrown while writing the NCEPH_EVENT_ACK message
 * 
 * 																		   ACK_RECEIVED
 * 	3.1)	Acknowledge the receipt of Ack (for EventData 1)try {
			 FileChannel channel = new RandomAccessFile(file, lockingMode).getChannel();
			 // Acquire an exclusive lock on this channel's file (blocks until lock can be retrieved)
			 lock = channel.lock();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		-------------------------------->   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 																										Update this (ackReceivedOn)
 * 
 * 																	   ACK_RECEIVED_AGAIN
 * 	3.2)	Ack already received and processed (for EventData 1)	-------------------------------->   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 			In case when cerebrum does not receive 
 * 			ACK_RECEIVED message due to any reasons 
 * 			like crash or network failure. And it sends 
 * 			the NCEPH_EVENT_ACK_AGAIN message as a 
 * 			part of its connector monitoring thread.
 * 																						
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public class ProofOfDelivery extends Document
{
	private int portNumber;
	
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
	
	private int acknowledgementAttempts = 0;
	
	private int publishAttempts = 0;
	
	private int threeWayAckAttempts = 0;
	
	private int deletePodAttempts = 0;
	
	private int subscriberCount;
	
	private PodState podState;
	
	private ConcurrentHashMap<Integer, ProofOfRelay> pors;
	
	private AtomicInteger relayCount;
	
	//acknowledgementStatus
	
	public ProofOfDelivery(){
		super.changeLog = new ArrayList<String>();
	}
	
	ProofOfDelivery(String messageId, EventData event, long createdOn, int portNumber)
	{
		this.createdOn = createdOn;
		this.messageId = messageId;
		this.event = event;
		this.portNumber = portNumber;
		super.changeLog = new ArrayList<String>();
		this.podState = PodState.INITIAL;
		this.relayCount = new AtomicInteger(0);
		changeLog.add("New");
	}
	
	@Override
	public String localMessageStoreLocation() 
	{
		// TODO Auto-generated method stub
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location");
	}
	
	@Override
	public String getName() {
		return "POD";
	}
	
	public int getPortNumber() {
		return portNumber;
	}

	public String getMessageId() {
		return messageId;
	}
	
	public EventData getEvent() {
		return event;
	}
	
	public IORecord getWriteRecord() {
		return writeRecord;
	}

	public void setWriteRecord(IORecord writeRecord) 
	{
		this.writeRecord = writeRecord;
		outOfSync("WriteRecord");
	}

	public IORecord getReadRecord() {
		return readRecord;
	}

	public void setReadRecord(IORecord readRecord) 
	{
		this.readRecord = readRecord;
		outOfSync("ReadRecord");
	}

	public NetworkRecord getAckNetworkRecord() {
		return ackNetworkRecord;
	}

	public void setAckNetworkRecord(NetworkRecord ackNetworkRecord) 
	{
		this.ackNetworkRecord = ackNetworkRecord;
		outOfSync("AckNetworkRecord");
	}

	public NetworkRecord getThreeWayAckNetworkRecord() {
		return threeWayAckNetworkRecord;
	}

	public void setThreeWayAckNetworkRecord(NetworkRecord threeWayAckNetworkRecord) 
	{
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

	public int getAcknowledgementAttempts() {
		return acknowledgementAttempts;
	}

	public int getPublishAttempts() {
		return publishAttempts;
	}

	public int getThreeWayAckAttempts() {
		return threeWayAckAttempts;
	}

	public int getDeletePodAttempts() {
		return deletePodAttempts;
	}
	
	public void incrementPublishAttempts() {
		this.publishAttempts++;
		outOfSync("publishAttempts");
	}
	
	public void incrementAcknowledgementAttempts() {
		this.acknowledgementAttempts++;
		outOfSync("acknowledgementAttempts");
	}
	
	public void incrementThreeWayAckAttempts() {
		this.threeWayAckAttempts++;
		outOfSync("threeWayAckAttempts");
	}
	
	public void incrementDeletePodAttempts() {
		this.deletePodAttempts++;
		outOfSync("deletePodAttempts");
	}
	
	public void decrementPublishAttempts() {
		this.publishAttempts--;
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
	
	public void decrementDeletePodAttempts() {
		this.deletePodAttempts--;
		outOfSync("deletePodAttempts");
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

	public void setAckReadRecord(IORecord ackReadRecord) 
	{
		this.ackReadRecord = ackReadRecord;
		outOfSync("AckReadRecord");
	}

	public IORecord getThreeWayAckWriteRecord() {
		return threeWayAckWriteRecord;
	}

	public void setThreeWayAckWriteRecord(IORecord threeWayAckWriteRecord) 
	{
		this.threeWayAckWriteRecord = threeWayAckWriteRecord;
		outOfSync("ThreeWayAckWriteRecord");
	}

	public IORecord getThreeWayAckReadRecord() {
		return threeWayAckReadRecord;
	}

	public void setThreeWayAckReadRecord(IORecord threeWayAckReadRecord) 
	{
		this.threeWayAckReadRecord = threeWayAckReadRecord;
		outOfSync("ThreeWayAckReadRecord");
	}


	public ConcurrentHashMap<Integer, ProofOfRelay> getPors() {
		return pors;
	}

	public void setPors(ConcurrentHashMap<Integer, ProofOfRelay> pors) 
	{
		this.pors = pors;
		outOfSync("Map of PORs");
	}
	
	public int getSubscriberCount() {
		return subscriberCount;
	}

	public void setSubscriberCount(int subscriberCount) 
	{
		this.subscriberCount = subscriberCount;
		outOfSync("SubscriberCount");
	}


	public PodState getPodState() {
		return podState;
	}

	public void setPodState(PodState podState) {
		this.podState = podState;
		outOfSync("PodState");
	}

	public AtomicInteger getRelayCount() {
		return relayCount;
	}

	public void incrementRelayCount() {
		this.relayCount.incrementAndGet();
		outOfSync("incrementRelayCount");
	}

	public void decrementRelayCount() {
		this.relayCount.decrementAndGet();
		outOfSync("decrementRelayCount");
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
		if(podState.getState() == 100 || podState.getState() == 200)
			decrementPublishAttempts();
		else
			decrementThreeWayAckAttempts();
	}
	
	/**
	 * 
	 */
	public void addPor(int port, ProofOfRelay por)
	{
		if(this.getPors() == null) // Create new POR hashmap if the message is not relayed to any of the subscriber so far
		{
			ConcurrentHashMap<Integer, ProofOfRelay> porHashMap = new ConcurrentHashMap<Integer, ProofOfRelay>();
			porHashMap.put(port, por);
			this.setPors(porHashMap);
		}
		else 
			this.getPors().put(port, por);
	}
	
	/**
	 * 
	 * @return String status
	 */
	public String validate()
	{
		String status = "";
		// BAD CODE - need to change this to use JSON Schema
		if(this.createdOn == 0L)
			status = "CreatedOn: NULL";

		if(this.getReadRecord() == null)
			status = status + ", ReadRecord: NULL";
		else if(this.getReadRecord().getStart() == 0L)
			status = status + ", ReadRecord.start: NULL";
		else if(this.getReadRecord().getEnd() == 0L)
			status = status + ", ReadRecord.end: NULL";

		if(this.getWriteRecord() == null)
			status = status + ", WriteRecord: NULL";
		else if(this.getWriteRecord().getStart() == 0L)
			status = status + ", WriteRecord.start: NULL";
		else if(this.getWriteRecord().getEnd() == 0L)
			status = status + ", WriteRecord.end: NULL";

		if(this.getAckNetworkRecord() == null)
			status = status + ", AckNetworkRecord: NULL";
		else if(this.getAckNetworkRecord().getStart() == 0L)
			status = status + ", AckNetworkRecord.start: NULL";
		else if(this.getAckNetworkRecord().getEnd() == 0L)
			status = status + ", AckNetworkRecord.end: NULL";

		if(this.getThreeWayAckNetworkRecord() == null)
			status = status + ", ThreeWayAckNetworkRecord: NULL";
		else if(this.getThreeWayAckNetworkRecord().getStart() == 0L)
			status = status + ", ThreeWayAckNetworkRecord.start: NULL";
		else if(this.getThreeWayAckNetworkRecord().getEnd() == 0L)
			status = status + ", ThreeWayAckNetworkRecord.end: NULL";
		
		if (this.relayCount.intValue() != this.subscriberCount) {
			status = status + ", Not Relayed to all subscriber";
		}
		
		return status;
	}
	
	public static class Builder
	{
		private int portNumber;
		
		private String messageId;
		
		private EventData event;
		
		private long createdOn;
		
		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
			return this;
		}
		
		public Builder portNumber(int portNumber)
		{
			this.portNumber = portNumber;
			return this;
		}
		
		public Builder event(EventData event)
		{
			this.event = event;
			return this;
		}
		
		public Builder createdOn(long createdOn)
		{
			this.createdOn = createdOn;
			return this;
		}
		
		public ProofOfDelivery build()
		{
			return new ProofOfDelivery(messageId, event, this.createdOn == 0L? new Date().getTime() : this.createdOn, portNumber);
		}
	}

}
