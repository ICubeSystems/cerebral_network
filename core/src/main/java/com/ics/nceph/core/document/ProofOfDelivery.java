package com.ics.nceph.core.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.ConnectorMonitorThread;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.MessageReader;
import com.ics.nceph.core.message.MessageWriter;
import com.ics.nceph.core.message.NetworkRecord;
import com.ics.nceph.core.receptor.Receptor;

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
 * 			<li>POD is created ({@link ProofOfDelivery#event event}, 
 * 				{@link ProofOfDelivery#createdOn createdOn}, 
 * 				{@link ProofOfDelivery#readRecord readRecord}, 
 * 				{@link ProofOfDelivery#eventNetworkRecord eventNetworkRecord}, 
 * 				{@link ProofOfDelivery#publishAttempts publishAttempts}, 
 * 				{@link ProofOfDelivery#acknowledgementAttempts acknowledgementAttempts}, 
 * 				{@link ProofOfDelivery#podState podState}) and saved on the local document storage on the cerebrum</li>
 * 			<li>Acknowledgement message NCEPH_EVENT_ACK is sent back to the synapse</li>
 * 			<li>Relay is initiated, for details please refer to {@link ProofOfRelay}</li>
 * 		</ol>
 * 	<li>Synaptic node receives the NCEPH_EVENT_ACK message and invokes {@link Receptor EventAcknowledgementReceptor} within a {@link SynapticReader} worker thread. 
 * 		As a part of EventAcknowledgementReceptor process:
 *  	<ol>
 * 			<li>POD is loaded from the local document store on the synapse</li>
 * 			<li>POD is updated ({@link ProofOfDelivery#readRecord readRecord}, 
 * 				{@link ProofOfDelivery#ackReadRecord ackReadRecord}, 
 * 				{@link ProofOfDelivery#eventNetworkRecord eventNetworkRecord},
 * 				{@link ProofOfDelivery#ackNetworkRecord ackNetworkRecord},
 * 				{@link ProofOfDelivery#acknowledgementAttempts acknowledgementAttempts},
 * 				{@link ProofOfDelivery#threeWayAckAttempts threeWayAckAttempts},
 * 				{@link ProofOfDelivery#podState podState}) and saved on the local document store</li>
 * 			<li>3Way Acknowledgement message ACK_RECEIVED is sent back to the cerebrum (acknowledgement of the acknowledgement message)</li>
 * 		</ol>
 * 	</li>
 * 	<li>Cerebrum receives the ACK_RECEIVED message and invokes {@link Receptor PublishedEventThreeWayAcknowledgementReceptor} within a {@link CerebralReader} worker thread. 
 * 		As a part of PublishedEventThreeWayAcknowledgementReceptor process:
 * 		<ol>
 * 			<li>POD is loaded from the local document store on the cerebrum</li>
 * 			<li>POD is updated ({@link ProofOfDelivery#threeWayAckReadRecord threeWayAckReadRecord}, 
 * 				{@link ProofOfDelivery#writeRecord writeRecord}, 
 * 				{@link ProofOfDelivery#threeWayAckNetworkRecord threeWayAckNetworkRecord},
 * 				{@link ProofOfDelivery#ackNetworkRecord ackNetworkRecord},
 * 				{@link ProofOfDelivery#threeWayAckAttempts threeWayAckAttempts},
 * 				{@link ProofOfDelivery#deletePodAttempts deletePodAttempts},
 * 				{@link ProofOfDelivery#podState podState}) and saved on the local document store</li>
 * 			<li>DELETE_POD message is sent back to the synapse which instructs the synapse to delete the POD from their local document store</li>
 * 		</ol>
 * 	</li>	
 * 	<li>Synaptic node receives the DELETE_POD message and invokes {@link Receptor DeletePodReceptor} within a {@link SynapticReader} worker thread.
 * 		As a part of DeletePodReceptor process:
 *  	<ol>
 * 			<li>POD is loaded from the local document store on the synapse</li>
 * 			<li>POD is updated ({@link ProofOfDelivery#threeWayAckNetworkRecord threeWayAckNetworkRecord}, 
 * 				{@link ProofOfDelivery#deletePodAttempts deletePodAttempts}, 
 * 				{@link ProofOfDelivery#podState podState}) and saved on the local document store. Update to POD is only required in case the delete operation fails</li>
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
 * 	<li><b>INITIAL | PUBLISHED:</b> 
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
public class ProofOfDelivery extends Document
{
	private int portNumber;
	
	/**
	 * Actual application {@link EventData data} of the event
	 */
	private EventData event;
	
	/**
	 * Time taken by {@link MessageWriter} on the synapse to write the PUBLISH_EVENT message on the socket channel
	 */
	private IORecord writeRecord;
	
	/**
	 * Time taken by {@link MessageReader} on the cerebrum to read the PUBLISH_EVENT message from the socket channel
	 */
	private IORecord readRecord;
	
	/**
	 * Time taken by {@link MessageWriter} on the cerebrum to write the NCEPH_EVENT_ACK message on the socket channel
	 */
	private IORecord ackWriteRecord;
	
	/**
	 * Time taken by {@link MessageReader} on the synapse to read the NCEPH_EVENT_ACK message from the socket channel
	 */
	private IORecord ackReadRecord;
	
	/**
	 * Time taken by {@link MessageWriter} on the synapse to write the ACK_RECEIVED message on the socket channel
	 */
	private IORecord threeWayAckWriteRecord;
	
	/**
	 * Time taken by {@link MessageReader} on the cerebrum to read the ACK_RECEIVED message from the socket channel
	 */
	private IORecord threeWayAckReadRecord;
	
	/**
	 * Network latency time for the {@link Message PUBLISH_EVENT} message from synapse to cerebrum
	 */
	private NetworkRecord eventNetworkRecord;
	
	/**
	 * Network latency time for the {@link Message NCEPH_EVENT_ACK} message from cerebrum to synapse
	 */
	private NetworkRecord ackNetworkRecord;
	
	/**
	 * Network latency time for the {@link Message ACK_RECEIVED} message from synapse to cerebrum
	 */
	private NetworkRecord threeWayAckNetworkRecord;
	
	/**
	 * Number of time {@link Message NCEPH_EVENT_ACK} message was sent from cerebrum to synapse
	 */
	private int acknowledgementAttempts = 0;
	
	/**
	 * Number of time {@link Message PUBLISH_EVENT} message was sent from synapse to cerebrum
	 */
	private int publishAttempts = 0;
	
	/**
	 * Number of time {@link Message ACK_RECEIVED} message was sent from synapse to cerebrum
	 */
	private int threeWayAckAttempts = 0;
	
	/**
	 * Number of time {@link Message DELETE_POD} message was sent from cerebrum to synapse
	 */
	private int deletePodAttempts = 0;
	
	/**
	 * Number of subscribers for this message
	 */
	private int subscriberCount;
	
	/**
	 * Current state of the POD
	 */
	private PodState podState;
	
	/**
	 * Map of subscribers and their {@link ProofOfRelay} 
	 */
	private ConcurrentHashMap<Integer, ProofOfRelay> pors;
	
	/**
	 * Number of subscribers the message was relayed completely
	 */
	private AtomicInteger relayCount;
	
	
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
