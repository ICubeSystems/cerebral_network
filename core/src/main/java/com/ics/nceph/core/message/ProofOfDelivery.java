package com.ics.nceph.core.message;

import java.util.Date;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.event.Event;

/**
 * Data structure to hold the complete information regarding the complex process of message delivery.
 * 
 * Scenario 1: The perfect world.
 * 	Step 1: Synaptic node creates a POD (createdOn) for the event to emit and saves it in its local file storage & then emits the event (PUBLISH_EVENT). 
 * 			CreatedOn is set in the event object (for Cerebrum)
 * 	Step 2: Cerebrum receives it and creates a POD (createdOn, ackSentOn) in its local storage. And sends back the acknowledgement (NCEPH_EVENT_ACK).
 * 	Step 3: Synaptic node on receiving the acknowledgement message sends acknowledgement received message (ACK_RECEIVED) to cerebrum with networkRecords & acknowledgementRecords
 * 	Step 4:	Cerebrum receives the acknowledgement received message (ACK_RECEIVED) and update the POD (networkRecords & acknowledgementRecords). 
 * 			Then it sends DELETE_POD message back to sender.
 * 	Step 5: Synaptic node deletes the POD from its local storage
 * 
 * 							Synaptic Node																	Cerebral Node
 * 
 * 													  				 	  PUBLISH_EVENT
 * 	1)					Event 1 (Gift created) 		  			--------------------------------> 		Event Message Received
 * 						Create POD (createdOn, writeRecord)		
 * 
 * 																		  NCEPH_EVENT_ACK
 * 	2)					Ack received (for Event 1)	  			<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Update POD (ackNetworkRecord)													Create POD (createdOn, readRecord, ackNetworkRecord.start)
 * 	
 * 																		   ACK_RECEIVED
 * 	3)		Acknowledge the receipt of Ack (for Event 1)		-------------------------------->   	ACK_RECEIVED Message Received
 * 			Update POD (3wayAckNetworkRecord.start)														Update POD (writeRecord, ackNetworkRecord, 3wayAckNetworkRecord)
 * 
 * 																		   DELETE_POD
 * 	4)			Delete the POD from the local storage			<--------------------------------   	Send DELETE_POD message back to sender
 * 																										Update POD (DeleteReqTime)
 * 																										
 * 
 * ===========================================================================================================================================
 * 
 * Scenario 2: Synaptic node does not receive acknowledgement message (NCEPH_EVENT_ACK) from cerebrum. This may be due to any of the reasons like crash or network failure.
 * 	Step 1: Synaptic node creates a POD (createdOn) for the event to emit and saves it in its local file storage & then emits the event (PUBLISH_EVENT). 
 * 			CreatedOn is set in the event object (for Cerebrum)
 * 	Step 2: Cerebrum receives it and creates a POD (createdOn, ackSentOn) in its local storage. And sends back the acknowledgement (NCEPH_EVENT_ACK).
 * 	Step 3: Synaptic node does not receive the acknowledgement message (NCEPH_EVENT_ACK) from cerebrum. Either it crashes or there is a network failure
 * 
 * Scenario 2.1 (System crash):  Synaptic node reboots after the crash
 * 	Step 4: During the bootstraping process it checks for PODs on the local storage. If there are any PODs then they are queued again to be resent.
 * 
 *							Synaptic Node																	Cerebral Node
 * 
 * 													  				 	  PUBLISH_EVENT
 * 	1)					Event 1 (Gift created) 		  			--------------------------------> 		Message Received
 * 						Create POD (createdOn)		
 * 
 * 																		 NCEPH_EVENT_ACK
 * 	2)					System Crash (Hardware/ software)		<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 																										Create POD (createdOn, ackSentOn, ackAttempt = 1)
 * 
 * 																		  PUBLISH_EVENT
 * 	3)					System Reboot							--------------------------------> 		Message Received		  
 * 						Resend messages with undeleted PODs 	
 * 
 *   																   NCEPH_EVENT_ACK_AGAIN
 * 	4)					Ack received (for Event 1) 				<-------------------------------- 		Re-Acknowledge the receipt of the PUBLISH_EVENT message to the sender		  
 * 																										Update POD (ackSentOn, ackAttempt++)
 * 
 *  																	   ACK_RECEIVED
 * 	5)		Acknowledge the receipt of Ack (for Event 1)		-------------------------------->   	ACK_RECEIVED Message Received
 * 																										Update POD (networkRecords & acknowledgementRecords)
 * 
 * 																		   DELETE_POD
 * 	6)			Delete the POD from the local storage			<--------------------------------   	Send DELETE_POD message back to sender
 * 																										
 *
 * Scenario 2.2 (Network failure):  The cerebral monitor thread periodically checks for the PODs on the local storage
 * 	Step 4: If the PODs are not deleted for a pre-defined time then they are queued again to be resent.   
 * 
 * 							Synaptic Node																	Cerebral Node
 * 
 * 													  				 	  PUBLISH_EVENT
 * 	1)					Event 1 (Gift created) 		  			--------------------------------> 		Message Received
 * 						Create POD (createdOn)		
 * 
 * 																		NCEPH_EVENT_ACK
 * 	2)					Network timeout (failure/ congestion)	<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Connection closed																Create POD (createdOn, ackSentOn, ackAttempt = 1)
 * 
 * 																		  PUBLISH_EVENT
 * 	3)					System Reboot							--------------------------------> 		Message Received		  
 * 						Resend messages with undeleted PODs 	
 * 
 *   																   NCEPH_EVENT_ACK_AGAIN
 * 	4)					Acknowledge the receipt of Ack 			<-------------------------------- 		Re-Acknowledge the receipt of the PUBLISH_EVENT message to the sender		  
 * 																										Update POD (ackSentOn, ackAttempt++)
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
 * 	1)					Event 1 (Gift created) 		  			--------------------------------> 		Message Received
 * 						Create POD (createdOn)		
 * 
 * 																		 NCEPH_EVENT_ACK
 * 	2.1)				Ack received (for Event 1)	  			<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Delete POD 																		Create POD (createdOn, ackSentOn)
 * 	
 * 																	   NCEPH_EVENT_ACK_AGAIN
 * 	2.2)				Ack received (for Event 1)	  			<--------------------------------   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 						Delete POD 																		Create POD (createdOn, ackSentOn)
 * 																										In case when the PUBLISH_EVENT message is sent again due to 
 * 																										fail over of the synaptic node. Or connector monitoring thread finds out 
 * 																										ACK_RECEIVED message is not received for more than a specified time period.
 * 																										Or RelayTimeoutException is thrown while writing the NCEPH_EVENT_ACK message
 * 
 * 																		   ACK_RECEIVED
 * 	3.1)	Acknowledge the receipt of Ack (for Event 1)		-------------------------------->   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
 * 																										Update POD (ackReceivedOn)
 * 
 * 																	   ACK_RECEIVED_AGAIN
 * 	3.2)	Ack already received and processed (for Event 1)	-------------------------------->   	Acknowledge the receipt of the PUBLISH_EVENT message to the sender
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
public class ProofOfDelivery 
{
	//private int sourceId;
	
	private String messageId;
	
	private Event event;
	
	private Date createdOn;
	
	private IORecord writeRecord;
	
	private IORecord readRecord;
	
	//ArrayList<networkRecord> emissionRecords
	
	//ArrayList<networkRecord> acknowledgementRecords
	
	//acknowledgementAttempts
	
	//acknowledgementStatus
	
	//IORecord
	//NetworkRecord
	
	ProofOfDelivery(){}
	
	ProofOfDelivery(String messageId, Event event, Date createdOn)
	{
		this.createdOn = createdOn;
		this.messageId = messageId;
		this.event = event;
	}
	
	public String getMessageId() {
		return messageId;
	}
	
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public Event getEvent() {
		return event;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}

	public Date getCreatedOn() {
		return createdOn;
	}
	
	public IORecord getWriteRecord() {
		return writeRecord;
	}

	public void setWriteRecord(IORecord writeRecord) {
		this.writeRecord = writeRecord;
	}

	public IORecord getReadRecord() {
		return readRecord;
	}

	public void setReadRecord(IORecord readRecord) {
		this.readRecord = readRecord;
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
	
	public static class Builder
	{
		private String messageId;
		
		private Event event;
		
		private Date createdOn;
		
		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
			return this;
		}
		
		public Builder event(Event event)
		{
			this.event = event;
			return this;
		}
		
		public Builder createdOn(Date createdOn)
		{
			this.createdOn = createdOn;
			return this;
		}
		
		public ProofOfDelivery build()
		{
			return new ProofOfDelivery(messageId, event, this.createdOn == null? new Date() : this.createdOn);
		}
	}
}
