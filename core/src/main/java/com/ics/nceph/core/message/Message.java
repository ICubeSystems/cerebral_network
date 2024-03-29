package com.ics.nceph.core.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.id.exception.IdGenerationFailedException;
import com.ics.nceph.core.db.document.store.ConfigStore;
import com.ics.nceph.core.db.document.store.IdStore;
import com.ics.nceph.core.message.data.MessageData;
import com.ics.util.ByteUtil;

/**
 * Base class for all the messages flowing in and out of the Encephelon server/ network.
 * 
 * Following is the message header structure (application layer protocol)
 * <pre>
 * 
 *    0         8        16        24        32
 *    +---------+---------+---------+---------+
 *    | Genesis |M_counter|  flags  | M_type  |
 *    +---------+---------+---------+---------+
 *    |      Node_id      |     Message_id    |
 *    +---------+---------+---------+---------+
 *    |               Message_id              |
 *    +---------+---------+---------+---------+
 *    |         Time_for_first_byte           |
 *    +---------+---------+---------+---------+
 *    |         Time_for_first_byte           |
 *    +---------+---------+---------+---------+
 *    |                M_length               |
 *    +---------+---------+---------+---------+
 *    |  Originator_port  |                   |
 *    +---------+---------+                   .
 *    |                                       .
 *    .            ...  Data ...              .
 *    .                                       .
 *    .                                       .
 *    +----------------------------------------
 * </pre>
 * 
 * 26 Bytes header with following sections:
 * <ol>
 * 	<li><b>Genesis</b> - 1 byte. Fixed value of -127.</li>
 * 	<li><b>Message Counter</b> - 1 Byte. Starting at 0, incremented every time a message is sent on the connection. 
 * 	  	At the receiving end it is compared by the counter at the receiver. It is reset to 0 after 255 messages.</li>
 *  <li><b>Flags</b> - 1 byte. 8 bit positions with following configurations:
 *  	<ul>
 *  		<li><b>Position 1: Tracing flag</b> - If a request support tracing and the tracing flag was set, 
 *  			the response/ acknowledgement to this request will be sent back with tracing information</li>
 *  		<li><b>Position 1: Compression flag</b> - If set, the message body is compressed</li>
 *  		<li><b>Position 1: Compression flag</b> - If set, the message body is compressed</li>
 *  	</ul>
 *  </li>
 *  <li><b>Message Type</b> - 1 byte. Following are the supported message incomingMessageType:<br>
 *  	<b>Synaptic message incomingMessageType</b> - messages generated via synaptic nodes (consumed by cerebral node):
 *  	<ul>
 *  		<li><b>0: STARTUP</b> - Initialize the connection. 
 *  			The server will respond by AUTHENTICATE message (in which case credentials will need to be provided using CREDENTIALS). 
 *  			This must be the first message of the connection. Once the connection has been initialized, a client should not send any more STARTUP message</li>
 *  		<li><b>1: CREDENTIALS</b> - Authenticate the connection.  
 *  			Synaptic node provide the credentials for the purpose of authentication. This message comes as a response to an AUTHENTICATE message from the server. 
 *  			The response to a CREDENTIALS is a READY message (or an ERROR message)</li>
 *  		<li><b>2: SUBSCRIBE</b> - Subscribe for a particular event id.  
 *  			Register this connection to receive a type of events. The body of the message is a [string list] representing the event ids to subscribe to. 
 *  			The response to a REGISTER message will be a READY message.</li>
 *  		<li><b>3: PUBLISH_EVENT</b> - Synaptic node (micro service/ application) publishes an event</li>
 *  		<li><b>4: RELAYED_EVENT_ACK</b> - Synaptic node acknowledges the receipt of the relayed event</li>
 *  		<li><b>5: ACK_RECEIVED</b> - Synaptic node acknowledges the receipt of the NCEPH_EVENT_ACK message</li>
 *  		<li><b>13: POR_DELETED</b> - Synaptic node sends a notification that relayv event acknowledged successfully and POR is deleted from snaptic side.</li>
 *  		<li><b>14: READY_CONFIRM</b> - Synaptic node send READY_CONFIRM message to Cerebrum then cerebrum relay message to Synaptic node.</li>
 *  		<li><b>15: BOOTSTRAP</b> - Synaptic node send BOOTSTRAP message to Cerebrum , which contains MAC address of Synapse</li>

 *  	</ul>
 *  	<b>Cerebral message incomingMessageType</b> - messages generated via cerebral node (consumed by synaptic nodes):
 *  	<ul>
 *  		<li><b>6: AUTHENTICATE</b> - Indicates that the Cerebrum require authentication.
 *  			This will be sent following a STARTUP message and must be answered by a CREDENTIALS message from the client.</li>
 *  		<li><b>7: READY</b> - Indicates that the server is ready to receive & process events via this connection.  
 *  			This message will be sent by the server either after a successful CREDENTIALS message or a successful SUBSCRIBE message. 
 *  			The body of a READY message is empty</li>
 *  		<li><b>8: AUTH_ERROR</b> - Indicates an error processing a request.  
 *  			The body of the message will be an error code ([int]) followed by a [string] error message. 
 *  			Then, depending on the exception, more content may follow.</li>
 *  		<li><b>9: NCEPH_EVENT_ACK</b> - Acknowledge the receipt of the PUBLISH_EVENT message on the Cerebrum</li>
 *  		<li><b>10: DELETE_POD</b> - Acknowledge the receipt of the PUBLISH_EVENT message on the Cerebrum</li>
 *  		<li><b>11: RELAY_EVENT</b> - Relay of PUBLISH_EVENT message to the subscriber synaptic nodes</li>
 *  		<li><b>12: RELAY_ACK_RECEIVED</b> - Acknowledge the event source regarding the receipt of the relayed event by the subscriber synaptic nodes</li>
 *  		<li><b>16: CONFIG</b> - Cerebrum node send CONFIG message to Synapse with following data:
 *  				<ul>
 *  					<li>NodeId of MAC address from name_records.json file</li>
 *  					<li>List of ApplicationReceptors for the event types subscribed by the node</li>
 *  				</ul>
 *  	</ul>
 *  	<b>Backpressure message </b> - Messages generated via consumer to notify producer to pause/resume sending messages:
 *  	<ul>
 *  		<li><b>17: PAUSE_TRANSMISSION</b> - Tell the producer to pause sending messages </li>
 *  		<li><b>18: RESUME_TRANSMISSION</b> - Tell the producer to resume sending messages </li>
 *  	</ul>
 *  </li>
 *  <li><b>Node Id</b> - 2 byte. Unique identifier of the node where the message is originating from. 
 *  	Node id & Message id together make the identification of the message unique across the Nceph network</li>
 *  <li><b>Message Id</b> - 6 byte. When sending request messages, this message id must be set by the client. 
 *  	This will be unique for every message per node (client application/ nceph server)</li>
 * 	<li><b>Message Length</b> - 4 byte. Length of the message body in number of bytes</li>
 * <li><b>Originating Port</b> - 2 byte. Originating location of the message</li>
 *  <li><b>TimeStamp</b> - 8 byte. timestamp of the message when it starts writing to the connection</li>
 * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 31-Dec-2021
 * @implNote To filter message logs use : ^((?!1-75).)*\R
 */
public class Message
{
	// @TODO: Pick this value from a configuration file on the node. This will be verified by the nceph server during the bootstrapping process of the node.
	private IORecord readRecord;
	
	private IORecord writeRecord;
	
	private final byte genesis = (byte)-127;
	
	byte counter;
	
	byte eventType;
	
	byte type;
	
	byte[] sourceId = new byte[2];
	
	byte[] messageId = new byte[6];
	
	byte[] dataLength = new byte[4];
	
	byte[] timeStamp = new byte[8];
	
	byte[] originatingPort = new byte[2];
	
	byte[] data;
	
	private Decoder decoder;
	
	/**
	 * This constructor should only be used to re-create the message object during the collection process at the receiving end
	 *  
	 * @param counter
	 * @param eventType
	 * @param type
	 * @param sourceId
	 * @param messageId
	 * @param dataLength
	 * @param data
	 */
	Message(byte counter, byte eventType, byte type, byte[] sourceId, byte[] messageId, byte[] dataLength, byte[] data, IORecord readRecord, byte[] timestamp, byte[] originatingPort )
	{
		this.counter = counter;
		this.eventType = eventType;
		this.type = type;
		this.sourceId = sourceId;
		this.messageId = messageId;
		this.dataLength = dataLength;
		this.data = data;
		this.readRecord = readRecord;
		this.timeStamp = timestamp;
		this.originatingPort = originatingPort;
	}
	
	
	/**
	 * 
	 * @param eventType
	 * @param type
	 * @param data
	 * @throws IdGenerationFailedException 
	 */
	Message(byte eventType, byte type, byte[] data, byte[] originatingPort) throws IdGenerationFailedException
	{
		init(eventType, type, data, null, null, originatingPort);
	}
	
	/**
	 * 
	 * @param eventType
	 * @param type
	 * @param data
	 * @param messageId
	 * @param sourceId
	 * @throws IdGenerationFailedException 
	 */
	protected Message(byte eventType, byte type, byte[] data, byte[] messageId, byte[] sourceId, byte[] originatingPort)
	{
		try 
		{
			init(eventType, type, data, messageId, sourceId, originatingPort);
		} catch (IdGenerationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void init(byte eventType, byte type, byte[] data, byte[] messageId, byte[] sourceId, byte[] originatingPort) throws IdGenerationFailedException
	{
		this.type = type;
		this.eventType = eventType;
		this.data = data;
		
		// Generate the message id from the message counter. This will be unique for the node.
		this.messageId = (messageId != null) ? messageId : ByteUtil.convertToByteArray(IdStore.getInstance().getId((decoder().getType() == 11 || decoder().getType() == 3) ? 100 : 200), this.messageId.length);
		
		// Set the Id of the source node where this message is originating from. 
		this.sourceId = (sourceId != null) ? sourceId : ByteUtil.convertToByteArray(ConfigStore.getInstance().getNodeId(), this.sourceId.length);
		
		// Set the dataLength
		dataLength = ByteUtil.convertToByteArray(data.length, dataLength.length);
		
		//set originatingPort
		this.originatingPort = originatingPort;
	}

	/**
	 * This method merges all the messages components and returns a ByteBuffer object
	 * 
	 * @throws IOException
	 * @return ByteBuffer
	 */
	public ByteBuffer toByteBuffer() throws IOException
	{
		// 1. Create new ByteArrayOutputStream to merge all the segments of the message
		ByteArrayOutputStream messageOutputStream = new ByteArrayOutputStream();
		
		// 2. Get the Merged ByteArray with all the message segments
		byte[] mergedBytes = ByteUtil.merge(
				ByteUtil.merge(
						genesis // Genesis byte [1 Bytes] 
						, counter // Message counter within the connection [1 Bytes]
						, eventType // eventType [1 Bytes]
						, type) // Message Type [1 Bytes] 
				, sourceId // Source id (node id which is creating the message) [2 Bytes]
				, messageId // Message id [6 Bytes]
				, timeStamp // timestamp [8 Bytes]
				, originatingPort // originatingPort [2 Bytes]
				, dataLength // Data Length [4 Bytes]
				, data); //Actual Data
		
		// 3. Create the ByteBuffer
		ByteBuffer buffer = ByteBuffer.wrap(mergedBytes);
		
		// 4. Close the ByteArrayOutputStream
		messageOutputStream.close();
		
		return buffer;
	}

	public Decoder decoder()
	{
		return (decoder == null) ? new Decoder() : decoder;
	}
	
	public byte getCounter() {
		return counter;
	}

	public byte getFlags() {
		return eventType;
	}

	public byte getType() {
		return type;
	}

	public byte[] getSourceId() {
		return sourceId;
	}

	public byte[] getMessageId() {
		return messageId;
	}
	
	public byte[] getOriginatingPort()
	{
		return originatingPort;
	}

	public byte[] getDataLength() {
		return dataLength;
	}

	public byte[] getData() {
		return data;
	}
	
	public void setCounter(byte counter) {
		this.counter = counter;
	}
	
	public void setTimeStamp(byte[] timeStamp) {
		this.timeStamp = timeStamp;
	}
	
	public void setOriginatingPort(byte[] originatingPort) {
		this.originatingPort = originatingPort;
	}
	
	public IORecord getReadRecord() {
		return readRecord;
	}

	public IORecord getWriteRecord() {
		return writeRecord;
	}

	public void setWriteRecord(IORecord writeRecord) 
	{
		// This can only be set once in lifetime of a message
		if (this.writeRecord == null)
			this.writeRecord = writeRecord;
	}

	public void setType(byte type) {
		this.type = type;
	}

	/**
	 * Inner class to get the decoded values from the message object
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 07-Mar-2022
	 */
	public class Decoder
	{
		public long getMessageId()
		{
			return ByteUtil.convertToLong(Message.this.messageId);
		}
		
		public int getSourceId()
		{
			return ByteUtil.convertToInt(Message.this.sourceId);
		}
		
		public int getType()
		{
			return ByteUtil.convertToInt(Message.this.type);
		}
		
		public int getDataLength()
		{
			return ByteUtil.convertToInt(Message.this.dataLength);
		}
		
		public int getCounter()
		{
			return ByteUtil.convertToInt(Message.this.counter);
		}
		
		public long getTimestamp()
		{
			return ByteUtil.convertToLong(Message.this.timeStamp);
		}
		
		public int getOriginatingPort()
		{
			return ByteUtil.convertToInt(Message.this.originatingPort);
		}
		
		public int geteventType()
		{
			return ByteUtil.convertToInt(Message.this.eventType);
		}
		
		public Object getData(Class<? extends MessageData> dataHoldingClass) throws JsonProcessingException
		{
			// Get the JSON string form byte array
			String json = ByteUtil.toObjectJSON(Message.this.data);
			// Get the EventData object from the JSON string
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json, dataHoldingClass);
		}

		/**
		 * This method returns the unique identifier of the message. It is constructed by concatenating sourceId/ nodeId & messageId. <br>
		 * eg. 123-25415261426 (123 is the synaptic node id from where the message was originated. 25415261426 is the message counter of the originating synaptic node)
		 * 
		 * @return String id of the message
		 */
		public String getId()
		{
			return new StringBuilder()
				.append(getSourceId())
				.append("-")
				.append(getMessageId())
				.toString();
		}
	}
}