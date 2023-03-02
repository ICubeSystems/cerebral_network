package com.ics.nceph.core.db.document;

import java.util.ArrayList;
import java.util.Date;

import org.springframework.stereotype.Component;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.db.document.exception.DocumentSaveFailedException;
import com.ics.nceph.core.db.document.store.DocumentStore;
import com.ics.nceph.core.db.document.store.cache.DocumentCache;
import com.ics.nceph.core.db.document.store.cache.MessageCache;
import com.ics.nceph.core.db.repository.PublishedMessageRepository;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.util.ApplicationContextUtils;

import lombok.Getter;

/**
 * <p>Data structure to hold the complete information of the complex process of publishing an event as message (PUBLISH_EVENT) to cerebrum. 
 * [Note: Relay of the message from cerebrum to all the subscriber nodes (synapses) is handled by {@link ProofOfRelay} data structure]<br>
 * </p>
 * <p>
 * <b>The perfect world</b><br>
 * Following is the process involved in end to end delivery of a message from synapse to cerebrum:
 * <ol>
 * 	<li>Synaptic application via synapseSDK would emit an event (for java, use - <code>Emitter.emit(event)</code>). As a part of emit protocol:
 * 		<ol>
 * 			<li>POD is created and saved on the local document storage on the synapse</li>
 * 			<li>Send a {@link Message PUBLISH_EVENT} message on the network</li>
 * 		</ol>
 * 	</li>
 * 	<li>Cerebrum receives the PUBLISH_EVENT message and invokes {@link Receptor PublishedEventReceptor} within a {@link CerebralReader} worker thread. 
 * 		As a part of PublishedEventReceptor process:
 * 		<ol>
 * 			<li>POD is created ({@link ProofOfPublish#event event}, 
 * 				{@link ProofOfPublish#createdOn createdOn}, 
 * 				{@link ProofOfPublish#readRecord readRecord}, 
 * 				{@link ProofOfPublish#eventNetworkRecord eventNetworkRecord}, 
 * 				{@link ProofOfPublish#eventPublishAttempts eventPublishAttempts}, 
 * 				{@link ProofOfPublish#acknowledgementAttempts acknowledgementAttempts}, 
 * 				{@link ProofOfPublish#messageDeliveryState messageDeliveryState}) and saved on the local document storage on the cerebrum</li>
 * 			<li>Acknowledgement message NCEPH_EVENT_ACK is sent back to the synapse</li>
 * 			<li>Relay is initiated, for details please refer to {@link ProofOfRelay}</li>
 * 		</ol>
 * 	<li>Synaptic node receives the NCEPH_EVENT_ACK message and invokes {@link Receptor EventAcknowledgementReceptor} within a {@link SynapticReader} worker thread. 
 * 		As a part of EventAcknowledgementReceptor process:
 *  	<ol>
 * 			<li>POD is loaded from the local document store on the synapse</li>
 * 			<li>POD is updated ({@link ProofOfPublish#readRecord readRecord}, 
 * 				{@link ProofOfPublish#ackReadRecord ackReadRecord}, 
 * 				{@link ProofOfPublish#eventNetworkRecord eventNetworkRecord},
 * 				{@link ProofOfPublish#ackNetworkRecord ackNetworkRecord},
 * 				{@link ProofOfPublish#acknowledgementAttempts acknowledgementAttempts},
 * 				{@link ProofOfPublish#threeWayAckAttempts threeWayAckAttempts},
 * 				{@link ProofOfPublish#messageDeliveryState messageDeliveryState}) and saved on the local document store</li>
 * 			<li>3Way Acknowledgement message ACK_RECEIVED is sent back to the cerebrum (acknowledgement of the acknowledgement message)</li>
 * 		</ol>
 * 	</li>
 * 	<li>Cerebrum receives the ACK_RECEIVED message and invokes {@link Receptor PublishedEventThreeWayAcknowledgementReceptor} within a {@link CerebralReader} worker thread. 
 * 		As a part of PublishedEventThreeWayAcknowledgementReceptor process:
 * 		<ol>
 * 			<li>POD is loaded from the local document store on the cerebrum</li>
 * 			<li>POD is updated ({@link ProofOfPublish#threeWayAckReadRecord threeWayAckReadRecord}, 
 * 				{@link ProofOfPublish#writeRecord writeRecord}, 
 * 				{@link ProofOfPublish#threeWayAckNetworkRecord threeWayAckNetworkRecord},
 * 				{@link ProofOfPublish#ackNetworkRecord ackNetworkRecord},
 * 				{@link ProofOfPublish#threeWayAckAttempts threeWayAckAttempts},
 * 				{@link ProofOfPublish#deleteAttempts deleteAttempts},
 * 				{@link ProofOfPublish#messageDeliveryState messageDeliveryState}) and saved on the local document store</li>
 * 			<li>DELETE_POD message is sent back to the synapse which instructs the synapse to delete the POD from their local document store</li>
 * 		</ol>
 * 	</li>	
 * 	<li>Synaptic node receives the DELETE_POD message and invokes {@link Receptor DeletePodReceptor} within a {@link SynapticReader} worker thread.
 * 		As a part of DeletePodReceptor process:
 *  	<ol>
 * 			<li>POD is loaded from the local document store on the synapse</li>
 * 			<li>POD is updated ({@link ProofOfPublish#threeWayAckNetworkRecord threeWayAckNetworkRecord}, 
 * 				{@link ProofOfPublish#deleteAttempts deleteAttempts}, 
 * 				{@link ProofOfPublish#messageDeliveryState messageDeliveryState}) and saved on the local document store. Update to POD is only required in case the delete operation fails</li>
 * 			<li>POD is deleted from the local document store</li>
 * 		</ol>
 * 	</li>
 * </ol>
 * 
 * <pre>
 * ╔════╦═══════════════════════════════════════╦═══════════════════════════════════╦════════════════════════════════════════╗
 * ║    ║             Synaptic Node             ║                                   ║              Cerebral Node             ║
 * ╠════╬═══════════════════════════════════════╬═══════════════════════════════════╬════════════════════════════════════════╣
 * ║    ║                                       ║           PUBLISH_EVENT           ║                                        ║
 * ║ 1) ║ Emit Event 1 (Gift created)           ║ --------------------------------> ║ EventData Message Received             ║
 * ║    ║ Create POD in the local doc store     ║                                   ║                                        ║
 * ╠════╬═══════════════════════════════════════╬═══════════════════════════════════╬════════════════════════════════════════╣
 * ║    ║                                       ║          NCEPH_EVENT_ACK          ║                                        ║
 * ║ 2) ║ Ack received (for Event 1)            ║ <-------------------------------- ║ Send the acknowledgement to the sender ║
 * ║    ║ Update the POD                        ║                                   ║ Create POD in the local doc store      ║
 * ╠════╬═══════════════════════════════════════╬═══════════════════════════════════╬════════════════════════════════════════╣
 * ║    ║                                       ║            ACK_RECEIVED           ║                                        ║
 * ║ 3) ║ Acknowledge the Acknowledgement       ║ --------------------------------> ║ ACK_RECEIVED Message Received          ║
 * ║    ║ Update the POD                        ║                                   ║ Update the POD                         ║
 * ╠════╬═══════════════════════════════════════╬═══════════════════════════════════╬════════════════════════════════════════╣
 * ║    ║                                       ║             DELETE_POD            ║                                        ║
 * ║ 4) ║ Delete the POD from the local storage ║ <-------------------------------- ║ Send DELETE_POD message to synapse     ║
 * ╚════╩═══════════════════════════════════════╩═══════════════════════════════════╩════════════════════════════════════════╝
 * </pre>
 * 
 * Events which have an impact the on the above process:
 * <ol>
 * 	<li>System crash (synapse or cerebrum) due to hardware or software failures</li>
 * 	<li>System restart (synapse or cerebrum) due to scheduled maintenance</li>
 * 	<li>Slow network due to network congestion</li>
 * </ol>
 * 
 * <p>To enforce the <b>reliability (guaranteed delivery of all the messages)</b> of the NCEPH network, 
 * {@link ConnectorMonitorThread Monitor} threads are instantiated on each node of the NCEPH network. 
 * <ul>
 * 	<li><b>On Cerebrum:</b> there is a {@link ConnectorMonitorThread CerebralMonitor} thread per {@link Connector}</li>
 * 	<li><b>On Synapse:</b> there is a single {@link ConnectorMonitorThread SynapticMonitor} thread</li>
 * </ul>
 * 
 * As a part of synaptic bootstrapping, SynapticMonitor thread is instantiated and executed. 
 * It checks for PODs which have exceeded the {@link Configuration#APPLICATION_PROPERTIES transmission.window} configuration and process them as per their state:
 * <ol>
 * 	<li><b>INITIAL | DELIVERED:</b> 
 * 		<p><b>Possible scenarios</b> resulting this state:
 * 		<ul>
 * 			<li>Synapse crash while sending PUBLISH_EVENT message</li>
 * 			<li>Cerebrum crash before receiving PUBLISH_EVENT message</li>
 * 			<li>Synapse crash before receiving NCEPH_EVENT_ACK message</li>
 * 			<li>Cerebrum crash before sending NCEPH_EVENT_ACK message</li>
 * 		</ul>
 * 		<b>Corrective Action:</b> re-send PUBLISH_EVENT message</p></li>
 * 		<br>
 * 	<li><b>ACKNOWLEDGED | ACK_RECIEVED:</b> 
 *  	<p><b>Possible scenarios</b> resulting this state:
 * 		<ul>
 * 			<li>Synapse crash before sending ACK_RECIEVED message</li>
 * 			<li>Cerebrum crash before receiving ACK_RECIEVED message</li>
 * 			<li>Synapse crash before receiving DELETE_POD message</li>
 * 			<li>Cerebrum crash before sending DELETE_POD message</li>
 * 		</ul>
 * 		<b>Corrective Action:</b> re-send ACK_RECIEVED message</p></li>
 * 		<br>
 * 	<li><b>FINISHED:</b> 
 * 		<p><b>Only Possible scenario</b> resulting this state:
 * 		<ul>
 * 			<li>Synapse receives DELETE_POD and change the state to finished but crashes before deleting the POD</li>
 * 			<li>Synapse receives DELETE_POD and change the state to finished but could not delete the POD due to some error</li>
 * 		</ul>
 * 		<b>Corrective Action:</b> Delete the POD from local storage</p></li>
 * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
@Getter
@DynamoDBDocument
@Component
public class ProofOfPublish extends ProofOfDelivery
{
	public static final String NAME = "POP";
	/**
	 * Number of subscribers for this message
	 */
	private int subscriberCount;

	/**
	 * Set of subscribers Port and their {@link ProofOfRelay}
	 */
	private ArrayList<Integer> subscribedPorts;

	/**
	 * Number of subscribers the message was relayed completely
	 */
	private Integer relayCount;

	public ProofOfPublish() {
		super();
	}

	ProofOfPublish(String messageId, EventData event, long createdOn, int producerPortNumber, int producerNodeId) 
	{
		super();
		setCreatedOn(createdOn);
		setMessageId(messageId);
		setProducerPortNumber(producerPortNumber);
		setProducerNodeId(producerNodeId);
		setEvent(event);
		setMessageDeliveryState(MessageDeliveryState.INITIAL.getState());
		this.relayCount = 0;
		this.subscribedPorts = new ArrayList<Integer>();
		getChangeLog().add("New");
	}

	public void setSubscribedPorts(ArrayList<Integer> createdPors)
	{
		this.subscribedPorts = createdPors;
		outOfSync("Set of subscribers port");
	}


	public void setSubscriberCount(int subscriberCount)
	{
		this.subscriberCount = subscriberCount;
		outOfSync("SubscriberCount");
	}

	
	public void setRelayCount(Integer relayCount)
	{
		this.relayCount = relayCount;
		outOfSync("relayCount");
	}

	public void incrementRelayCount()
	{
		this.relayCount++;
		outOfSync("incrementRelayCount");
	}

	public void decrementRelayCount()
	{
		this.relayCount--;
		outOfSync("decrementRelayCount");
	}

	public void decrementAttempts()
	{
		if (getMessageDeliveryState() == 100 || getMessageDeliveryState() == 200)
			decrementEventMessageAttempts();
		else
			decrementThreeWayAckMessageAttempts();
	}

	public void addSubscribedPort(int port)
	{
		subscribedPorts.add(port);
		outOfSync("addCreatedPors: "+port);
	}
	
	public void finished() 
	{
		//If the message has been fully relayed to all the intended subscriber then set the state and remove it from the cerebral cache
		if(getRelayCount() == getSubscriberCount() && getMessageDeliveryState() == MessageDeliveryState.FINISHED.getState()) 
		{
			setMessageDeliveryState( MessageDeliveryState.FULLY_RELAYED.getState() );
			try
			{
				// Update document
				DocumentStore.getInstance().update(this, getMessageId());
				// Remove document from cache.
				removeFromCache();
			} catch (DocumentSaveFailedException e){}
		}
	}
	/**
	 * 
	 * @return String status
	 */
	public String validate()
	{
		String status = "";
		// BAD CODE - need to change this to use JSON Schema
		if (getCreatedOn() == 0L)
			status = "CreatedOn: NULL";

		if (this.getEventMessageReadRecord() == null)
			status = status + ", ReadRecord: NULL";
		else if (this.getEventMessageReadRecord().getStart() == 0L)
			status = status + ", ReadRecord.start: NULL";
		else if (this.getEventMessageReadRecord().getEnd() == 0L)
			status = status + ", ReadRecord.end: NULL";

		if (this.getEventMessageWriteRecord() == null)
			status = status + ", WriteRecord: NULL";
		else if (this.getEventMessageWriteRecord().getStart() == 0L)
			status = status + ", WriteRecord.start: NULL";
		else if (this.getEventMessageWriteRecord().getEnd() == 0L)
			status = status + ", WriteRecord.end: NULL";

		if (this.getAckMessageNetworkRecord() == null)
			status = status + ", AckNetworkRecord: NULL";
		else if (this.getAckMessageNetworkRecord().getStart() == 0L)
			status = status + ", AckNetworkRecord.start: NULL";
		else if (this.getAckMessageNetworkRecord().getEnd() == 0L)
			status = status + ", AckNetworkRecord.end: NULL";

		if (this.getThreeWayAckMessageNetworkRecord() == null)
			status = status + ", ThreeWayAckNetworkRecord: NULL";
		else if (this.getThreeWayAckMessageNetworkRecord().getStart() == 0L)
			status = status + ", ThreeWayAckNetworkRecord.start: NULL";
		else if (this.getThreeWayAckMessageNetworkRecord().getEnd() == 0L)
			status = status + ", ThreeWayAckNetworkRecord.end: NULL";

		if (this.relayCount.intValue() != this.subscriberCount)
		{
			status = status + ", Not Relayed to all subscriber";
		}

		return status;
	}

	public static class Builder
	{
		private int producerPortNumber;

		private int producerNodeId;

		private String messageId;

		private EventData event;

		private long createdOn;

		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
			return this;
		}

		public Builder producerPortNumber(int producerPortNumber)
		{
			this.producerPortNumber = producerPortNumber;
			return this;
		}

		public Builder producerNodeId(int producerNodeId)
		{
			this.producerNodeId = producerNodeId;
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

		public ProofOfPublish build()
		{
			return new ProofOfPublish(messageId, event, this.createdOn == 0L ? new Date().getTime() : this.createdOn,
					producerPortNumber, producerNodeId);
		}
	}

	@Override
	public String localRepository()
	{
		return Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location")
				+ String.valueOf(getProducerPortNumber()) + "/";
	}
	
	@Override
	public void saveInCache()
	{
		DocumentCache.getInstance()
			.getPublishedMessageCache()
			.put(getProducerPortNumber(), this);
	}

	@Override
	public void removeFromCache()
	{
		DocumentCache.getInstance()
			.getPublishedMessageCache()
			.removeFromCache(getProducerPortNumber(), this);
		if(Boolean.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("messages.removeLocalCompletedMessages"))) {
			DocumentStore.getInstance().delete(getMessageId(), this);
		}
	}

	@Override
	public void saveInDB() throws DocumentSaveFailedException 
	{
		// Generate key and set
		setKey(Key.<String, String>builder()
							.partitionKey("P:" + String.valueOf(getProducerPortNumber()))
							.sortKey(getMessageId())
							.build());

		try 
		{ // Save in DB
			ApplicationContextUtils.context.getBean("publishedMessageRepository", PublishedMessageRepository.class).save(this);
		} catch (Exception e) 
		{
			e.printStackTrace();
			throw new DocumentSaveFailedException("Publish message save failed Exception ", e);
		}
	}
	
	public static ProofOfPublish load(Integer producerPort, String docName)
	{
		try
		{
			return DocumentCache.getInstance().getPublishedMessageCache().getDocument(producerPort, docName);
		} catch (NullPointerException e){return null;}
	}
	
	public static MessageCache<ProofOfPublish> getMessageCache(Integer producerPort)
	{
		try
		{
			return DocumentCache.getInstance().getPublishedMessageCache().getMessageCache(producerPort);
		} catch (NullPointerException e){return null;}
	}
	
	@Override
	public void setMessageDeliveryState(Integer messageDeliveryState)
	{
		super.setMessageDeliveryState(messageDeliveryState);
	}
	
}
