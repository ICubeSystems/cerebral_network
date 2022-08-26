package com.ics.nceph.core.document;

import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.MessageReader;
import com.ics.nceph.core.message.MessageWriter;
import com.ics.nceph.core.message.NetworkRecord;

/**
 * 
 * @author Chandan Verma
 * @since 23-Aug-2022
 */
public abstract class ProofOfDelivery extends Document
{
	/**
	 * Message sender Port Number {@link producerPortNumber} 
	 */
	private Integer producerPortNumber;
	
	/**
	 * Message sender NodeID {@link producerNodeId} 
	 */
	private Integer producerNodeId;
	
	/**
	 * Boolean indicator to indicate if the object is upload to DB. Default set to false.
	 */
	private boolean messageInDB = false;
	
	/**
	* Actual application {@link EventData data} of the event
	*/
	private EventData event;
	
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
	 * Current state of the POD
	 */
	private MessageDeliveryState messageDeliveryState;
	
	
	public ProofOfDelivery() {}
	
	public EventData getEvent() 
	{
		return event;
	}
	
	public void setEvent(EventData event) 
	{
		this.event = event;
	}
	
	public boolean isMessageInDB() 
	{
		return messageInDB;
	}

	public void setMessageInDB(boolean messageInDB) 
	{
		this.messageInDB = messageInDB;
	}
	
	public Integer getProducerPortNumber() 
	{
		return producerPortNumber;
	}
	
	public void setProducerPortNumber(Integer producerPortNumber) 
	{
		this.producerPortNumber = producerPortNumber;
		outOfSync("ProducerPortNumber");
	}
	
	public Integer getProducerNodeId() 
	{
		return producerNodeId;
	}
	
	public void setProducerNodeId(Integer producerNodeId) 
	{
		this.producerNodeId = producerNodeId;
//		outOfSync("ProducerNodeId");
	}

	
	public IORecord getEventMessageWriteRecord() {
		return eventMessageWriteRecord;
	}

	public void setEventMessageWriteRecord(IORecord writeRecord) 
	{
		this.eventMessageWriteRecord = writeRecord;
		outOfSync("EventMessageWriteRecord");
	}

	public IORecord getEventMessageReadRecord() {
		return eventMessageReadRecord;
	}

	public void setEventMessageReadRecord(IORecord readRecord) 
	{
		this.eventMessageReadRecord = readRecord;
		outOfSync("EventMessageReadRecord");
	}
	
	public IORecord getAckMessageWriteRecord() {
		return ackMessageWriteRecord;
	}

	public void setAckMessageWriteRecord(IORecord ackWriteRecord) {
		this.ackMessageWriteRecord = ackWriteRecord;
		outOfSync("AckMessageWriteRecord");
	}
	
	public IORecord getAckMessageReadRecord() {
		return ackMessageReadRecord;
	}

	public void setAckMessageReadRecord(IORecord ackReadRecord) 
	{
		this.ackMessageReadRecord = ackReadRecord;
		outOfSync("AckMessageReadRecord");
	}

	public IORecord getThreeWayAckMessageWriteRecord() {
		return threeWayAckMessageWriteRecord;
	}

	public void setThreeWayAckMessageWriteRecord(IORecord threeWayAckWriteRecord) 
	{
		this.threeWayAckMessageWriteRecord = threeWayAckWriteRecord;
		outOfSync("ThreeWayAckMessageWriteRecord");
	}
	

	public IORecord getThreeWayAckMessageReadRecord() {
		return threeWayAckMessageReadRecord;
	}

	public void setThreeWayAckMessageReadRecord(IORecord threeWayAckReadRecord) 
	{
		this.threeWayAckMessageReadRecord = threeWayAckReadRecord;
		outOfSync("ThreeWayAckMessageReadRecord");
	}

	public NetworkRecord getEventMessageNetworkRecord() {
		return eventMessageNetworkRecord;
	}

	public void setEventMessageNetworkRecord(NetworkRecord eventNetworkRecord) {
		this.eventMessageNetworkRecord = eventNetworkRecord;
		outOfSync("EventMessageNetworkRecord");
	}
	
	public NetworkRecord getAckMessageNetworkRecord() {
		return ackMessageNetworkRecord;
	}

	public void setAckMessageNetworkRecord(NetworkRecord ackNetworkRecord) 
	{
		this.ackMessageNetworkRecord = ackNetworkRecord;
		outOfSync("AckMessageNetworkRecord");
	}
	
	public NetworkRecord getThreeWayAckMessageNetworkRecord() {
		return threeWayAckMessageNetworkRecord;
	}

	public void setThreeWayAckMessageNetworkRecord(NetworkRecord threeWayAckNetworkRecord) 
	{
		this.threeWayAckMessageNetworkRecord = threeWayAckNetworkRecord;
		outOfSync("ThreeWayAckMessageNetworkRecord");
	}
	
	public int getAcknowledgementMessageAttempts() {
		return acknowledgementMessageAttempts;
	}
	
	public void incrementAcknowledgementMessageAttempts() {
		this.acknowledgementMessageAttempts++;
		outOfSync("AcknowledgementMessageAttempts");
	}
	
	public void decrementAcknowledgementMessageAttempts() {
		this.acknowledgementMessageAttempts--;
		outOfSync("AcknowledgementMessageAttempts");
	}
	
	public int getThreeWayAckMessageAttempts() {
		return threeWayAckMessageAttempts;
	}
	
	public void incrementThreeWayAckMessageAttempts() {
		this.threeWayAckMessageAttempts++;
		outOfSync("ThreeWayAckMessageAttempts");
	}
	
	public void decrementThreeWayAckMessageAttempts() {
		this.threeWayAckMessageAttempts--;
		outOfSync("ThreeWayAckMessageAttempts");
	}
	
	public int getFinalMessageAttempts() {
		return finalMessageAttempts;
	}
	
	public void incrementFinalMessageAttempts() {
		this.finalMessageAttempts++;
		outOfSync("FinalMessageAttempts");
	}
	
	public void decrementFinalMessageAttempts() {
		this.finalMessageAttempts--;
		outOfSync("FinalMessageAttempts");
	}
	
	public MessageDeliveryState getMessageDeliveryState() {
		return messageDeliveryState;
	}

	public void setMessageDeliveryState(MessageDeliveryState messageDeliveryState) 
	{
		this.messageDeliveryState = messageDeliveryState;
		outOfSync("messageDeliveryState");
	}
	
	public int getEventMessageAttempts() {
		return eventMessageAttempts;
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
}
