package com.ics.nceph.core.connector.connection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.ssl.exception.SSLHandshakeException;

/**
 * SSLConnection class is used when nceph security mode is set to SSL/TLS
 * @author Anshul
 * @since 21-Mar-2022
 */
public class SSLConnection extends Connection 
{
	
	SSLEngine engine;
	
	/**
     * This buffer stores application data (plain text) to send over socket channel. This data will be encrypted 
     * using {@link SSLEngine#wrap(ByteBuffer, ByteBuffer)} and sent over the socket channel. This buffer can typically
     * be of any size, as long as it is large enough to contain the outgoing message.
     * If this node tries to send a message bigger than buffer's capacity a {@link BufferOverflowException}
     * will be thrown.
     */
    protected ByteBuffer appDataToWrite;//MYAPPDATA

    /**
     * This buffer stores encrypted data, that is generated after {@link SSLEngine#wrap(ByteBuffer, ByteBuffer) wrapping}.
     * It should be initialized using {@link SSLSession#getPacketBufferSize()},
     * which returns the size up to which, SSL/TLS packets will be generated from the engine under a session.
     * All SSLEngine network buffers should be sized at least this large to avoid insufficient space problems when performing wrap and unwrap calls.
     */
    protected ByteBuffer encryptedData;//MYNETDATA

    /**
     * This buffer is used by the reader to store the decrypted data after unwrapping. It must be large enough to hold the application data
     * from any peer. Can be initialized with {@link SSLSession#getApplicationBufferSize()} for an estimation
     * of the other peer's application data and should be enlarged if this size is not enough.
     */
    protected ByteBuffer decryptedData;//PEERAPPDATA

    /**
     * This buffer is used by the reader to store the encrypted data received via the socket channel. 
     * The SSL/TLS protocols specify that the implementations should produce packets containing at most 16 KB of plaintext,
     * so a buffer sized to this value should normally cause no capacity problems. However, some implementations violate the specification and generate large records up to 32 KB.
     * If the {@link SSLEngine#unwrap(ByteBuffer, ByteBuffer)} detects large inbound packets, the buffer sizes returned by SSLSession will be updated dynamically, so the receiving node 
     * should check for overflow conditions and enlarge the buffer using the session's (updated) buffer size.
     */
    protected ByteBuffer encryptedDataToRead;//PEERNETDATA

    /**
     * Will be used to execute tasks that may emerge during handshake in parallel with the server's main thread.
     */
    protected ExecutorService executor;
    
    int encryptedDataBufferSize;
    
    /**
	 * Constructs a connection for cerebral connector
	 * 
	 * @param id
	 * @param connector
	 * @param relayTimeout
	 * @param receiveBufferSize
	 * @param sendBufferSize
	 * @throws IOException
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 */
    SSLConnection(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize) throws IOException, ConnectionInitializationException
	{
		super(id, connector, relayTimeout, receiveBufferSize, sendBufferSize);
	}
	
	/**
	 * Constructs a connection for synaptic connector
	 * 
	 * @param id
	 * @param connector
	 * @param relayTimeout
	 * @param receiveBufferSize
	 * @param sendBufferSize
	 * @param cerebralConnectorAddress
	 * @throws IOException
	 * @throws ConnectionException 
	 * @throws ImproperReactorClusterInstantiationException
	 * @throws ReactorNotAvailableException
	 */
    SSLConnection(Integer id, Connector connector, Integer relayTimeout, Integer receiveBufferSize, Integer sendBufferSize, InetSocketAddress cerebralConnectorAddress) throws IOException, ConnectionInitializationException, ConnectionException
	{
		super(id, connector, relayTimeout, receiveBufferSize, sendBufferSize, cerebralConnectorAddress);
	}
	
	@Override
	protected void initializeConnection() throws IOException, SSLHandshakeException 
	{
		// Create ssl engine
		createSSLEngine();
		// Instantiate a single thread executer to run SSL engine delegated tasks
		executor = Executors.newSingleThreadExecutor();
		// perform handshake protocol before we can send and receive application data using the engine
		doHandshake();
		// TODO Auto-generated method stub
		super.initializeConnection();
	}
	
	private void createSSLEngine() throws SSLException 
	{
		// Create SSL engine to be used for wrapping unwrapping the data on this connection
		engine = getConnector().getSslContext().createSSLEngine();
		// Set client mode of the SSL engine
		engine.setUseClientMode(isClient);
		// Allocate size of buffers for wrapping and unwrapping process. 
		initializeSSEngineBuffers();
		// Initial SSL handshake
		engine.beginHandshake();
		
	}
	
	private void initializeSSEngineBuffers() 
	{
		// Get the engine session to get the buffer sizes defined in the engine
		SSLSession initialSession = engine.getSession();
		plainTextBufferSize = initialSession.getApplicationBufferSize();
		encryptedDataBufferSize = initialSession.getPacketBufferSize();
		initialSession.invalidate();
		// Allocate size of byte buffer and make them ready to use for handshake process.
		appDataToWrite = ByteBuffer.allocate(plainTextBufferSize);
		encryptedData = ByteBuffer.allocate(encryptedDataBufferSize);
		decryptedData = ByteBuffer.allocate(plainTextBufferSize);
        encryptedDataToRead = ByteBuffer.allocate(encryptedDataBufferSize);
	}
	
	/**
     * Implements the handshake protocol between two nodes, required for the establishment of the SSL/TLS connection.
     * During the handshake, encryption configuration information - such as the list of available cipher suites - will be exchanged
     * and if the handshake is successful, it will lead to an established SSL/TLS session.
     *
     * <p/>
     * A typical handshake will usually contain the following steps:
     *
     * <ul>
     *   <li>1. wrap:     ClientHello</li>
     *   <li>2. unwrap:   ServerHello/Cert/ServerHelloDone</li>
     *   <li>3. wrap:     ClientKeyExchange</li>
     *   <li>4. wrap:     ChangeCipherSpec</li>
     *   <li>5. wrap:     Finished</li>
     *   <li>6. unwrap:   ChangeCipherSpec</li>
     *   <li>7. unwrap:   Finished</li>
     * </ul>
     * <p/>
     * Handshake is also used during the end of the session, in order to properly close the connection between the two nodes.
     * A proper connection close will typically include the one node sending a CLOSE message to another, and then wait for
     * the other's CLOSE message to close the transport link. The other node from his perspective would read a CLOSE message
     * from his peer node and then enter the handshake procedure to send his own CLOSE message as well.
     *
     * @param socketChannel - the socket channel that connects the two nodes
     * @throws IOException - if an error occurs during read/write on the socket channel
	 * @throws SSLHandshakeException 
     */
    public void doHandshake() throws IOException, SSLHandshakeException 
    {
        SSLEngineResult result;
        HandshakeStatus handshakeStatus;

        int appBufferSize = engine.getSession().getApplicationBufferSize();
        ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
        ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
        encryptedData.clear();
        encryptedDataToRead.clear();
        
        handshakeStatus = engine.getHandshakeStatus();
        while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED && handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) 
        {
            switch (handshakeStatus) 
            {
	            case NEED_UNWRAP:
	                if (getSocket().read(encryptedDataToRead) < 0) {
	                    if (engine.isInboundDone() && engine.isOutboundDone()) {
	                        throw new SSLHandshakeException("SSL handshaking failure ");
	                    }
	                    try {
	                        engine.closeInbound();
	                    } catch (SSLException e) {
	                    	System.out.println("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
	                    }
	                    engine.closeOutbound();
	                    // After closeOutbound the engine will be set to WRAP state, in order to try to send a close message to the client.
	                    handshakeStatus = engine.getHandshakeStatus();
	                    break;
	                }
	                encryptedDataToRead.flip();
	                try {
	                    result = engine.unwrap(encryptedDataToRead, peerAppData);
	                    encryptedDataToRead.compact();
	                    handshakeStatus = result.getHandshakeStatus();
	                } catch (SSLException sslException) {
	                	System.out.println("A problem was encountered while processing the data that caused the SSLEngine to abort. Will try to properly close connection...");
	                    engine.closeOutbound();
	                    handshakeStatus = engine.getHandshakeStatus();
	                    break;
	                }
	                switch (result.getStatus()) {
	                case OK:
	                    break;
	                case BUFFER_OVERFLOW:
	                    // Will occur when peerAppData's capacity is smaller than the data derived from encryptedDataToRead's unwrap.
	                    peerAppData = enlargeBuffer(peerAppData);
	                    break;
	                case BUFFER_UNDERFLOW:
	                    // Will occur either when no data was read from the peer or when the encryptedDataToRead buffer was too small to hold all peer's data.
	                    encryptedDataToRead = handleBufferUnderflow(encryptedDataToRead);
	                    break;
	                case CLOSED:
	                    if (engine.isOutboundDone()) {
	                    	throw new SSLHandshakeException("SSL handshaking failure ");
	                    } else {
	                        engine.closeOutbound();
	                        handshakeStatus = engine.getHandshakeStatus();
	                        break;
	                    }
	                default:
	                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
	                }
	                break;
	            case NEED_WRAP:
	            	encryptedData.clear();
	                try 
	                {
	                    result = engine.wrap(myAppData, encryptedData);
	                    handshakeStatus = result.getHandshakeStatus();
	                } catch (SSLException sslException) 
	                {
	                	System.out.println("Exception occured while wrapping: "+ sslException.getMessage());
	                    engine.closeOutbound();
	                    //In case of SSLException handshakeStatus will be NOT_HANDSHAKING
	                    handshakeStatus = engine.getHandshakeStatus();
	                    break;
	                }
	                switch (result.getStatus()) {
	                case OK :
	                	encryptedData.flip();
	                    while (encryptedData.hasRemaining()) {
	                    	getSocket().write(encryptedData);
	                    }
	                    break;
	                case BUFFER_OVERFLOW:
	                    // Will occur if there is not enough space in encryptedData buffer to write all the data that would be generated by the method wrap.
	                    // Since encryptedData is set to session's packet size we should not get to this point because SSLEngine is supposed
	                    // to produce messages smaller or equal to that, but a general handling would be the following:
	                	encryptedData = enlargeBuffer(encryptedData);
	                    break;
	                case BUFFER_UNDERFLOW:
	                    throw new SSLException("Buffer underflow occured after a wrap. I don't think we should ever get here.");
	                case CLOSED:
	                    try {
	                    	encryptedData.flip();
	                        while (encryptedData.hasRemaining()) {
	                        	getSocket().write(encryptedData);
	                        }
	                        // At this point the handshake status will probably be NEED_UNWRAP so we make sure that encryptedDataToRead is clear to read.
	                        encryptedDataToRead.clear();
	                    } catch (Exception e) {
	                    	System.out.println("Failed to send server's CLOSE message due to socket channel's failure.");
	                        handshakeStatus = engine.getHandshakeStatus();
	                    }
	                    break;
	                default:
	                    throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
	                }
	                break;
	            case NEED_TASK:
	                Runnable task;
	                while ((task = engine.getDelegatedTask()) != null) {
	                    executor.execute(task);
	                }
	                handshakeStatus = engine.getHandshakeStatus();
	                break;
	            case FINISHED:
	                break;
	            case NOT_HANDSHAKING:
	                break;
	            default:
	                throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
            }
        }

    }


    /**
     * Compares <code>sessionProposedCapacity<code> with buffer's capacity. If buffer's capacity is smaller,
     * returns a buffer with the proposed capacity. If it's equal or larger, returns a buffer
     * with capacity twice the size of the initial one.
     *
     * @param buffer - the buffer to be enlarged.
     * @param sessionProposedCapacity - the minimum size of the new buffer, proposed by {@link SSLSession}.
     * @return A new buffer with a larger capacity.
     */
    public ByteBuffer enlargeBuffer(ByteBuffer buffer) 
    {
       
        return ByteBuffer.allocate(buffer.capacity() * 2);
    }

    /**
     * Handles {@link SSLEngineResult.Status#BUFFER_UNDERFLOW}. The {@code SSLEngine} was not able to unwrap the incoming data because there were not enough source bytes
     * available to make a complete packet. So this method is called to enlarge the encryptedDataToRead buffer size. Also since the buffer can contain some data which was read,
     * the data will be put to the new enlarged buffer before return
     *
     * @param buffer - will always be encryptedDataToRead buffer
     * @return The same buffer if there is no space problem or a new buffer with the same data but more space.
     * @throws Exception
     */
    public ByteBuffer handleBufferUnderflow(ByteBuffer buffer) 
    {
        if (engine.getSession().getPacketBufferSize() < buffer.limit()) 
            return buffer;
        
        ByteBuffer replaceBuffer = enlargeBuffer(buffer);
        buffer.flip();
        replaceBuffer.put(buffer);
        return replaceBuffer;
    }

    /**
     * This method should be called when this node wants to explicitly close the connection
     * or when a close message has arrived from the other node, in order to provide an orderly shutdown.
     * <p/>
     * It first calls {@link SSLEngine#closeOutbound()} which prepares this node to send its own close message and
     * sets {@link SSLEngine} to the <code>NEED_WRAP</code> state. Then, it delegates the exchange of close messages
     * to the handshake method and finally, it closes socket channel.
     *
     * @param socketChannel - the transport link used between the two peers.
     * @throws SocketException 
     * @throws IOException if an I/O error occurs to the socket channel.
     */
    public void closeConnection() throws SocketException  
    {
        engine.closeOutbound();  
		try {
			doHandshake();
		} catch (IOException | SSLHandshakeException e) {
			throw new SocketException("Exception in handshake while closing connection. ERROR:" + e.getMessage());
		}
    }
    
    /**
     * In addition to orderly shutdowns, an unorderly shutdown may occur, when the transport link (socket channel)
     * is severed before close messages are exchanged. This may happen by getting an -1 or {@link IOException}
     * when trying to read from the socket channel, or an {@link IOException} when trying to write to it.
     * In both cases {@link SSLEngine#closeInbound()} should be called and then try to follow the standard procedure.
     *
     * @throws IOException if an I/O error occurs to the socket channel.
     * @throws SocketException 
     */
    public void handleEndOfStream() throws SocketException  
    {
        try 
        {
        	System.out.println("This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream (-1)");
            engine.closeInbound();
        } catch (Exception e) {
        	System.out.println("Exception in engine.closeInbound()");
        }
        closeConnection();
        throw new SocketException("Connection closed: engine status - CLOSED");
    }

	public SSLEngine getEngine() {
		return engine;
	}
	
	public int getEncryptedDataBufferSize() {
		return encryptedDataBufferSize;
	}
}
