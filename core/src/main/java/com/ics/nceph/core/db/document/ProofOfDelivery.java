package com.ics.nceph.core.db.document;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIndexRangeKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.MessageReader;
import com.ics.nceph.core.message.MessageWriter;
import com.ics.nceph.core.message.NetworkRecord;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Chandan Verma
 * @since 23-Aug-2022
 */
@Getter
@Setter
@DynamoDBDocument
public abstract class ProofOfDelivery extends MessageDocument
{
	/**
	 * Message sender NodeID {@link producerNodeId} 
	 */
	private Integer producerNodeId;
	
	/**
	* Actual application {@link EventData data} of the event
	*/
	private EventData event;
	
	/**
	* Actual application {@link EventData eventType} of the event
	*/
	private Integer eventType;
	
	/**
	* Actual application {@link EventData eventId} of the event
	*/
	private Integer eventId;
	/**
	 * Time taken by {@link MessageWriter} on the synapse to write the PUBLISH_EVENT message on the socket channel
	 */
	private IORecord eventMessageWriteRecord;
	
	/**
	 * Time taken by {@link MessageReader} on the cerebrum to read the PUBLISH_EVENT message from the socket channel
	 */
	private IORecord eventMessageReadRecord;
	
	/**
	 * Time taken by {@link MessageWriter} on the cerebrum to write the NCEPH_EVENT_ACK message on the socket channel
	 */
	private IORecord ackMessageWriteRecord;
	
	/**
	 * Time taken by {@link MessageReader} on the synapse to read the NCEPH_EVENT_ACK message from the socket channel
	 */
	private IORecord ackMessageReadRecord;
	
	/**
	 * Time taken by {@link MessageWriter} on the synapse to write the ACK_RECEIVED message on the socket channel
	 */
	private IORecord threeWayAckMessageWriteRecord;
	
	/**
	 * Time taken by {@link MessageReader} on the cerebrum to read the ACK_RECEIVED message from the socket channel
	 */
	private IORecord threeWayAckMessageReadRecord;
	
	/**
	 * Network latency time for the {@link Message PUBLISH_EVENT} message from synapse to cerebrum
	 */
	private NetworkRecord eventMessageNetworkRecord;
	
	/**
	 * Network latency time for the {@link Message NCEPH_EVENT_ACK} message from cerebrum to synapse
	 */
	private NetworkRecord ackMessageNetworkRecord;
	
	/**
	 * Network latency time for the {@link Message ACK_RECEIVED} message from synapse to cerebrum
	 */
	private NetworkRecord threeWayAckMessageNetworkRecord;
	
	/**
	 * Number of time {@link Message NCEPH_EVENT_ACK} message was sent from cerebrum to synapse
	 */
	private int acknowledgementMessageAttempts = 0;
	
	/**
	 * Number of time {@link Message PUBLISH_EVENT} message was sent from synapse to cerebrum
	 */
	private int eventMessageAttempts = 0;
	
	/**
	 * Number of time {@link Message ACK_RECEIVED} message was sent from synapse to cerebrum
	 */
	private int threeWayAckMessageAttempts = 0;
	
	/**
	 * Number of time {@link Message DELETE_POD} message was sent from cerebrum to synapse
	 */
	private int finalMessageAttempts = 0;
	
	/**
	 * Delivery state of the message
	 */
	@DynamoDBIndexRangeKey(globalSecondaryIndexName = "action-messageDeliveryState-index")
	private Integer messageDeliveryState;
	
	/**
	 * action performed on document
	 */
	@DynamoDBIndexHashKey(globalSecondaryIndexName = "action-messageDeliveryState-index")
	private String action;
	
	
	public ProofOfDelivery() {
		super();
	}
	
	public void setEvent(EventData event) 
	{
		this.event = event;
		this.eventType = event.getEventType();
		this.eventId = event.getEventId();
	}
	
	@DynamoDBIgnore
	@JsonIgnore
	public Integer getMid() {
		int mid = Integer.valueOf(getMessageId().split("-",2)[1]);
		return mid;
	}
	
	public void setProducerNodeId(Integer producerNodeId) 
	{
		this.producerNodeId = producerNodeId;
		outOfSync("ProducerNodeId");
	}

	public void setEventMessageWriteRecord(IORecord writeRecord) 
	{
		this.eventMessageWriteRecord = writeRecord;
		outOfSync("EventMessageWriteRecord");
	}

	public void setEventMessageReadRecord(IORecord readRecord) 
	{
		this.eventMessageReadRecord = readRecord;
		outOfSync("EventMessageReadRecord");
	}

	public void setAckMessageWriteRecord(IORecord ackWriteRecord) {
		this.ackMessageWriteRecord = ackWriteRecord;
		outOfSync("AckMessageWriteRecord");
	}

	public void setAckMessageReadRecord(IORecord ackReadRecord) 
	{
		this.ackMessageReadRecord = ackReadRecord;
		outOfSync("AckMessageReadRecord");
	}

	public void setThreeWayAckMessageWriteRecord(IORecord threeWayAckWriteRecord) 
	{
		this.threeWayAckMessageWriteRecord = threeWayAckWriteRecord;
		outOfSync("ThreeWayAckMessageWriteRecord");
	}

	public void setThreeWayAckMessageReadRecord(IORecord threeWayAckReadRecord) 
	{
		this.threeWayAckMessageReadRecord = threeWayAckReadRecord;
		outOfSync("ThreeWayAckMessageReadRecord");
	}

	public void setEventMessageNetworkRecord(NetworkRecord eventNetworkRecord) {
		this.eventMessageNetworkRecord = eventNetworkRecord;
		outOfSync("EventMessageNetworkRecord");
	}

	public void setAckMessageNetworkRecord(NetworkRecord ackNetworkRecord) 
	{
		this.ackMessageNetworkRecord = ackNetworkRecord;
		outOfSync("AckMessageNetworkRecord");
	}

	public void setThreeWayAckMessageNetworkRecord(NetworkRecord threeWayAckNetworkRecord) 
	{
		this.threeWayAckMessageNetworkRecord = threeWayAckNetworkRecord;
		outOfSync("ThreeWayAckMessageNetworkRecord");
	}
	
	public void incrementAcknowledgementMessageAttempts() {
		this.acknowledgementMessageAttempts++;
		outOfSync("AcknowledgementMessageAttempts");
	}
	
	public void decrementAcknowledgementMessageAttempts() {
		this.acknowledgementMessageAttempts--;
		outOfSync("AcknowledgementMessageAttempts");
	}
	
	public void incrementThreeWayAckMessageAttempts() {
		this.threeWayAckMessageAttempts++;
		outOfSync("ThreeWayAckMessageAttempts");
	}
	
	public void decrementThreeWayAckMessageAttempts() {
		this.threeWayAckMessageAttempts--;
		outOfSync("ThreeWayAckMessageAttempts");
	}
	
	public void incrementFinalMessageAttempts() {
		this.finalMessageAttempts++;
		outOfSync("FinalMessageAttempts");
	}
	
	public void decrementFinalMessageAttempts() {
		this.finalMessageAttempts--;
		outOfSync("FinalMessageAttempts");
	}

	public void incrementEventMessageAttempts()
	{
		this.eventMessageAttempts++;
		outOfSync("eventMessageAttempts");
	}

	public void decrementEventMessageAttempts() {
		this.eventMessageAttempts--;
		outOfSync("eventMessageAttempts");
	}
	
	public void setMessageDeliveryState(Integer messageDeliveryState) 
	{
		if(getMessageDeliveryState() == null || getMessageDeliveryState()< messageDeliveryState)
		{
			this.messageDeliveryState = messageDeliveryState;
			outOfSync("messageDeliveryState");
		}
	}
}
