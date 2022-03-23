package com.ics.nceph.core.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.event.Event;
import com.ics.util.ByteUtil;

/**
 * Base class for all the messages flowing in and out of the Encephelon server/ network.
 * 
 * Following is the message header structure (application layer protocol)
 * 
 *    0         8        16        24        32
 *    +---------+---------+---------+---------+
 *    | Genesis |M_counter|  flags  | M_type  |
 *    +---------+---------+---------+---------+
 *    |      Node_id      |     Message_id    |
 *    +---------+---------+---------+---------+
 *    |               Message_id              |
 *    +---------+---------+---------+---------+
 *    |                M_length               |
 *    +---------+---------+---------+---------+
 *    |                                       |
 *    .            ...  Data ...              .
 *    .                                       .
 *    .                                       .
 *    +----------------------------------------
 *    
 * 16 Bytes header with following sections:
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
 *  <li><b>Message Type</b> - 1 byte. Following are the supported message types:<br>
 *  	<b>Synaptic message types</b> - messages generated via synaptic nodes (consumed by cerebral node):
 *  	<ul>
 *  		<li><b>0x00: STARTUP</b> - Initialize the connection. 
 *  			The server will respond by AUTHENTICATE message (in which case credentials will need to be provided using CREDENTIALS). 
 *  			This must be the first message of the connection. Once the connection has been initialized, a client should not send any more STARTUP message</li>
 *  		<li><b>0x01: CREDENTIALS</b> - Authenticate the connection.  
 *  			Synaptic node provide the credentials for the purpose of authentication. This message comes as a response to an AUTHENTICATE message from the server. 
 *  			The response to a CREDENTIALS is a READY message (or an ERROR message)</li>
 *  		<li><b>0x02: SUBSCRIBE</b> - Subscribe for a particular event id.  
 *  			Register this connection to receive a type of events. The body of the message is a [string list] representing the event ids to subscribe to. 
 *  			The response to a REGISTER message will be a READY message.</li>
 *  		<li><b>0x03: PUBLISH_EVENT</b> - Synaptic node (micro service/ application) publishes an event</li>
 *  		<li><b>0x04: RELAYED_EVENT_ACK</b> - Synaptic node acknowledges the receipt of the relayed event</li>
 *  		<li><b>0x05: ACK_RECEIVED</b> - Synaptic node acknowledges the receipt of the relayed event</li>
 *  	</ul>
 *  	<b>Cerebral message types</b> - messages generated via cerebral node (consumed by synaptic nodes):
 *  	<ul>
 *  		<li><b>0x06: AUTHENTICATE</b> - Indicates that the Cerebrum require authentication.
 *  			This will be sent following a STARTUP message and must be answered by a CREDENTIALS message from the client.</li>
 *  		<li><b>0x07: READY</b> - Indicates that the server is ready to receive & process events via this connection.  
 *  			This message will be sent by the server either after a successful CREDENTIALS message or a successful SUBSCRIBE message. 
 *  			The body of a READY message is empty</li>
 *  		<li><b>0x08: ERROR</b> - Indicates an error processing a request.  
 *  			The body of the message will be an error code ([int]) followed by a [string] error message. 
 *  			Then, depending on the exception, more content may follow.</li>
 *  		<li><b>0x09: NCEPH_EVENT_ACK</b> - Acknowledge the receipt of the PUBLISH_EVENT message on the Cerebrum</li>
 *  		<li><b>0x0A: RELAY_EVENT</b> - Relay of PUBLISH_EVENT message to the subscriber synaptic nodes</li>
 *  		<li><b>0x0B: RELAY_ACK</b> - Acknowledge the event source regarding the receipt of the relayed event by the subscriber synaptic nodes</li>
 *  	</ul>
 *  </li>
 *  <li><b>Node Id</b> - 2 byte. Unique identifier of the node where the message is originating from. 
 *  	Node id & Message id together make the identification of the message unique across the Nceph network</li>
 *  <li><b>Message Id</b> - 6 byte. When sending request messages, this message id must be set by the client. 
 *  	This will be unique for every message per node (client application/ nceph server)</li>
 * 	<li><b>Message Length</b> - 4 byte. Length of the message body in number of bytes</li>
 * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 31-Dec-2021
 */
public class Message 
{
	// TODO this should be initialized by local datastore or a central server should be used for id generation
	private static AtomicInteger messageCounter = new AtomicInteger(1);
	
	// @TODO: Pick this value from a configuration file on the node. This will be verified by the nceph server during the bootstraping process of the node.
	private static final int NODE_ID = Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("node.id"));
	
	private IORecord readRecord;
	
	private IORecord writeRecord;
	
	private final byte genesis = (byte)-127;
	
	byte counter;
	
	byte flags;
	
	byte type;
	
	byte[] sourceId = new byte[2];
	
	byte[] messageId = new byte[6];
	
	byte[] dataLength = new byte[4];
	
	byte[] data;
	
	private Decoder decoder;
	
	/**
	 * This constructor should only be used to re-create the message object during the collection process at the receiving end
	 *  
	 * @param counter
	 * @param flags
	 * @param type
	 * @param sourceId
	 * @param messageId
	 * @param dataLength
	 * @param data
	 */
	Message(byte counter, byte flags, byte type, byte[] sourceId, byte[] messageId, byte[] dataLength, byte[] data, IORecord readRecord)
	{
		this.counter = counter;
		this.flags = flags;
		this.type = type;
		this.sourceId = sourceId;
		this.messageId = messageId;
		this.dataLength = dataLength;
		this.data = data;
		this.readRecord = readRecord;
	}
	
	/**
	 * 
	 * @param flags
	 * @param type
	 * @param data
	 */
	Message(byte flags, byte type, byte[] data)
	{
		this.type = type;
		this.flags = flags;
		this.data = data;
		
		// Generate the message id from the message counter. This will be unique for the node.
		long messageIdNumber = messageCounter.getAndIncrement();
		messageId = ByteUtil.convertToByteArray(messageIdNumber, messageId.length);
		
		// Set the Id of the source node where this message is originating from. 
		sourceId = ByteUtil.convertToByteArray(NODE_ID, sourceId.length);
		
		// Set the dataLength
		dataLength = ByteUtil.convertToByteArray(data.length, dataLength.length);
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
						, flags // Flags (8 bit) [1 Bytes]
						, type) // Message Type [1 Bytes] 
				, sourceId // Source id (node id which is creating the message) [2 Bytes]
				, messageId // Message id [6 Bytes]
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
		return flags;
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

	public byte[] getDataLength() {
		return dataLength;
	}

	public byte[] getData() {
		return data;
	}
	
	public void setCounter(byte counter) {
		this.counter = counter;
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
		
		public Object getData() throws JsonProcessingException
		{
			// Get the JSON string form byte array
			String json = ByteUtil.toObjectJSON(Message.this.data);
			// Get the Event object from the JSON string
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json, Event.class);
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