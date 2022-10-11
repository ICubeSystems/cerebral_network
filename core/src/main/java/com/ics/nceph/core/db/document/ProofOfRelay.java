package com.ics.nceph.core.db.document;

import java.util.Date;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.cache.DocumentCache;
import com.ics.nceph.core.db.document.store.cache.MessageCache;
import com.ics.nceph.core.db.repository.ReceivedMessageRepository;
import com.ics.nceph.core.event.EventData;
import com.ics.util.ApplicationContextUtils;

import lombok.Getter;
import lombok.Setter;

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
@Getter
@Setter
public class ProofOfRelay extends ProofOfDelivery
{
	
	public static final String NAME = "POR";
	/**
	 * Message Receiver NodeId {@link consumerNodeId}
	 */
	private Integer consumerNodeId;
	
	/**
	 * Message Receiver Port Number {@link consumerPortNumber}
	 */
	private Integer consumerPortNumber;
	
	/**
	 * ApplicationReceptor class received event 
	 */
	private String appReceptorName;
	
	/**
	 * ApplicationReceptor execution time
	 */
	private long appReceptorExecutionTime;
	
	/**
	 * ApplicationReceptor error message
	 */
	private String appReceptorExecutionErrorMsg;
	
	/**
	 * Execution attempts of ApplicationReceptor 
	 */
	private int appReceptorExecutionAttempts = 0;
	
	/**
	 * ApplicationReceptor execution failed status
	 */
	private boolean appReceptorFailed;
	
	public ProofOfRelay() {
		super();
	}
	
	ProofOfRelay(String messageId, EventData event, long createdOn, Integer nodeId, Integer consumerPort, Integer producerPort, Integer producerNodeId)
	{
		super();
		setCreatedOn(createdOn);
		setMessageId(messageId);
		setEvent(event);
		setMessageDeliveryState(MessageDeliveryState.INITIAL.getState());
		this.consumerNodeId = nodeId;
		this.consumerPortNumber = consumerPort;
		this.setProducerPortNumber(producerPort);
		this.setProducerNodeId(producerNodeId);
		getChangeLog().add("New");
	}
	
	public void setConsumerPortNumber(Integer consumerPortNumber) 
	{
		this.consumerPortNumber = consumerPortNumber;
		outOfSync("consumerPortNumber");
	}

	public void setAppReceptorName(String appReceptorName) {
		this.appReceptorName = appReceptorName;
		outOfSync("appReceptorName");
	}

	public void setAppReceptorExecutionTime(long appReceptorExecutionTime) {
		this.appReceptorExecutionTime = appReceptorExecutionTime;
		outOfSync("appReceptorExecutionTime");
	}

	public void setAppReceptorExecutionErrorMsg(String appReceptorExecutionErrorMsg) {
		this.appReceptorExecutionErrorMsg = appReceptorExecutionErrorMsg;
		outOfSync("appReceptorExecutionErrorMsg");
	}

	public void incrementAppReceptorExecutionAttempts() {
		this.appReceptorExecutionAttempts++;
		outOfSync("incrementAppReceptorExecutionAttempts");
	}
	
	public void setAppReceptorFailed(boolean appReceptorFailed)
	{
		this.appReceptorFailed = appReceptorFailed;
		outOfSync("appReceptorFailed");
	}

	public void setConsumerNodeId(Integer nodeId) 
	{
		this.consumerNodeId = nodeId;
		outOfSync("consumerNodeId");
	}

	public void decrementAttempts()
	{
		if(getMessageDeliveryState() == 100 || getMessageDeliveryState() == 200)
			decrementEventMessageAttempts();
		else
			decrementThreeWayAckMessageAttempts();
	}
	
	public void setAppReceptorExecutionAttempts(int appReceptorExecutionAttempts)
	{
		this.appReceptorExecutionAttempts = appReceptorExecutionAttempts;
		outOfSync("appReceptorExecutionAttempts");
	}
	
	public static class Builder
	{
		private String messageId;
		
		private EventData event;
		
		private int nodeId;
		
		private long relayedOn;
		
		private Integer consumerPort;
		
		private Integer producerPort;
		
		private Integer producerNodeId;
		
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
		
		public Builder consumerPort(int consumerPort)
		{
			this.consumerPort = consumerPort;
			return this;
		}
		
		public Builder producerPort(int producerPort)
		{
			this.producerPort = producerPort;
			return this;
		}
		
		public Builder producerNodeId(int producerNodeId)
		{
			this.producerNodeId = producerNodeId;
			return this;
		}
		
		public ProofOfRelay build()
		{
			return new ProofOfRelay(messageId, event, this.relayedOn  == 0L? new Date().getTime() : this.relayedOn, nodeId, consumerPort, producerPort, producerNodeId);
		}
	}
	
	@Override
	public String localRepository() 
	{
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.relayed_location")+String.valueOf(getProducerPortNumber())+"/"+consumerPortNumber+"/";
	}
	
	@Override
	public void saveInCache() 
	{
		DocumentCache.getInstance()
			.getRelayedMessageCache()
			.put(this);
	}
	
	@Override
	public void removeFromCache()
	{
		DocumentCache.getInstance()
			.getRelayedMessageCache()
			.removeFromCache(this);
	}
	
	@Override
	public void saveInDB() throws DocumentSaveFailedException
	{
		// 1. Generate key and set
		setKey(new Key.Builder()
						.partitionKey("R:" + String.valueOf(getConsumerPortNumber()))
						.sortKey(getMessageId())
						.build());
		try 
		{ 
			// Save in DB
			ApplicationContextUtils.context.getBean("receivedMessageRepository", ReceivedMessageRepository.class).save(this);
		} catch (ResourceNotFoundException | DynamoDBMappingException e) 
		{ 
			throw new DocumentSaveFailedException("Received message save failed Exception ", e); 
		}
	}
	
	public static ProofOfRelay load(Integer producerPort, Integer consumerPort, String docName)
	{
		try
		{
			return DocumentCache.getInstance().getRelayedMessageCache().getDocument(producerPort, consumerPort, docName);
		} catch (NullPointerException e){return null;}
	}

	public static MessageCache<ProofOfRelay> getMessageCache(Integer producerPort, Integer consumerPort) 
	{
		try
		{
			return DocumentCache.getInstance().getRelayedMessageCache().getMessageCache(producerPort, consumerPort);
		} catch (Exception e){return null;}
	}
}
