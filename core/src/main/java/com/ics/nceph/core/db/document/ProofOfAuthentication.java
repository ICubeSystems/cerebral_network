package com.ics.nceph.core.db.document;

import java.util.Date;

import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.db.document.store.cache.DocumentCache;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;
/**
 * 
 * Data structure to hold the complete information of connection.
 * 
 * Step 1: Synaptic node creates a STARTUP message and saves in local file storage and then send message to the cerebrum
 * Step 2: Cerebrum receive STARTUP message and save local file storage and then cerebrum create a AUTHENTICATE message and send to the Synaptic node
 * Step 3: Synaptic node receive AUTHENTICATION mesaage and update POA in local storage and then create CREDENTIALS message and send to the cerebrum
 * Step 4: Cerebrum receive CREDENTIALS message and check credentials if credentials is true then create READY message else credentials is false then create AUTH_ERROR message,
 * 		   update POA in local storage and send message to the Synaptic node
 *	  4.1: If credentials is true -- Set connection state READY and add connection in the ConnectionLoadBalancer and ActiveConnections, Send READY message to the Synaptic node
 *	  4.1: If credentials is false-- Set connection state AUTH_FAILED and connection teardown, Send AUTH_ERROR message to the Synaptic node
 * Step 5: Synaptic node receive READY or AUTH_ERROR message
 *    5.1: (READY message) -- Receive READY message and change connection state AUTH_PENDING to READY state, Add connection in the ConnectionLoadBalancer and ActiveConnections, 
 *    	   Create READYCONFIRM message and send to the cerebrum
 * 	  5.2: (AUTH_ERROR message) -- Receive AUTH_ERROR message change connection state AUTH_PENDING to AUTH_FAILED state and teardown the connection
 * Step 6: Cerebrum receive READYCONFIRM message, cerebrum check in the connector relayQueue if outgoing messages in the relayQueue then transfer them to the connections relayQueue
 * 
 * 
 * 						Synaptic Node																								Cerebrum
 * 
 * 													  				 			   STARTUP
 * 	1)		STARTUP Message send		  								--------------------------------> 			STARTUP Message Received
 * 			Create POA (createdOn, WriteRecord,																		Create POA (createdOn, readRecord, startupNetworkRecord)
 * 			statrupNetWorkRecord)
 * 
 * 																				  AUTHENTICATE
 * 	2)		AUTHENTICATION Message received  							<--------------------------------   		AUTHENTICATION Message send
 * 			Update POA (authenticationNetworkRecord, statrupNetworkRecord)											Update POA (authenticationNetworkRecord)
 * 
 * 																				   CREDENTIALS
 * 	3)		CREDENTIALS Message	send									-------------------------------->   	    CREDENTIALS Message Received
 * 			Update POA (credentialsNetworkRecord)																	Update POA (authenticationNetworkRecord, credentialsNetworkRecord) 
 * 			
 * 																				   READY,AUTH_ERROR
 * 	4)		IF Message is AUTH_ERROR -- Connection state AUTH_FAILED	<--------------------------------   	    READY message OR AUTH_ERROR message send
 * 			IF Message is READY -- Connection state READY															Update POA (readyNetworkRecord OR errorNetworkRecord)
 * 			Update POA (readyNetworkRecord OR errorNetworkRecord,
 * 				credentialsNetworkRecord)
 * 																				 READY_CONFIRMED
 *  5)		READY_CONFIRMED Message send								-------------------------------->   	    READY_CONFIRMED Message Received
 * 			Update POA ()																							Update POA ()
 * 
 * ============================================================================================================================================================================================
 */
public class ProofOfAuthentication extends MessageDocument 
{
	public static final String NAME = "POA";
		
	private IORecord startupWriteRecord;

	private IORecord startupReadRecord;

	private IORecord authenticationWriteRecord;

	private IORecord authenticationReadRecord;

	private IORecord credentialsWriteRecord;

	private IORecord credentialsReadRecord;

	private IORecord readyWriteRecord;

	private IORecord readyReadRecord;

	private IORecord readyConfirmedWriteRecord;
	
	private IORecord readyConfirmedReadRecord;
	
	private IORecord authenticationErrorWriteRecord;

	private IORecord authenticationErrorReadRecord;

	private NetworkRecord startupNetworkRecord;

	private NetworkRecord authenticationNetworkRecord;

	private NetworkRecord credentialsNetworkRecord;

	private NetworkRecord readyNetworkRecord;

	private NetworkRecord authenticationErrorNetworkRecord;
	
	private NetworkRecord readyConfirmedNetworkRecord;

	private PoaState poaState;
	
	private long deletePoaTime;
	
	private Integer messageDeliveryState;

	public ProofOfAuthentication() {
		super();
	}

	public ProofOfAuthentication(String messageId, long createdOn, Integer originatingPort) 
	{
		super();
		setMessageId(messageId);
		this.poaState = PoaState.INITIAL;
		setProducerPortNumber(originatingPort);
		setCreatedOn(createdOn);
		getChangeLog().add("New");
	}

	public NetworkRecord getAuthenticationNetworkRecord() 
	{
		return authenticationNetworkRecord;
	}

	public NetworkRecord getCredentialsNetworkRecord() 
	{
		return credentialsNetworkRecord;
	}

	public NetworkRecord getStartupNetworkRecord() {
		return startupNetworkRecord;
	}

	public void setStartupNetworkRecord(NetworkRecord startupNetworkRecord) {
		this.startupNetworkRecord = startupNetworkRecord;
		outOfSync("startupNetworkRecord");
	}

	public void setAuthenticationNetworkRecord(NetworkRecord authenticationNetworkRecord) 
	{
		this.authenticationNetworkRecord = authenticationNetworkRecord;
		outOfSync("authenticationNetworkRecord");
	}

	public void setCredentialsNetworkRecord(NetworkRecord credentialsNetworkRecord) 
	{
		this.credentialsNetworkRecord = credentialsNetworkRecord;
		outOfSync("credentialsNetworkRecord");
	}

	public IORecord getAuthenticationWriteRecord() {
		return authenticationWriteRecord;
	}

	public void setAuthenticationWriteRecord(IORecord authenticationWriteRecord) {
		this.authenticationWriteRecord = authenticationWriteRecord;
		outOfSync("authenticationWriteRecord");
	}

	public IORecord getAuthenticationErrorWriteRecord() {
		return authenticationErrorWriteRecord;
	}

	public void setAuthenticationErrorWriteRecord(IORecord authenticationErrorWriteRecord) {
		this.authenticationErrorWriteRecord = authenticationErrorWriteRecord;
		outOfSync("authenticationErrorWriteRecord");
	}

	public IORecord getAuthenticationErrorReadRecord() {
		return authenticationErrorReadRecord;
	}

	public void setAuthenticationErrorReadRecord(IORecord authenticationErrorReadRecord) {
		this.authenticationErrorReadRecord = authenticationErrorReadRecord;
		outOfSync("authenticationErrorReadRecord");
	}

	public NetworkRecord getAuthenticationErrorNetworkRecord() {
		return authenticationErrorNetworkRecord;
	}

	public void setAuthenticationErrorNetworkRecord(NetworkRecord authenticationErrorNetworkRecord) {
		this.authenticationErrorNetworkRecord = authenticationErrorNetworkRecord;
		outOfSync("authenticationErrorNetworkRecord");
	}

	public long getDeletePoaTime() 
	{
		return deletePoaTime;
	}

	public void setDeletePoaTime(long deletePoaTime) 
	{
		this.deletePoaTime = deletePoaTime;
		outOfSync("deletePoaTime");

	}

	public NetworkRecord getReadyNetworkRecord() 
	{
		return readyNetworkRecord;
	}

	public void setReadyNetworkRecord(NetworkRecord readyNetworkRecord) 
	{
		this.readyNetworkRecord = readyNetworkRecord;
		outOfSync("readyNetworkRecord");
	}

	public IORecord getAuthenticationReadRecord() 
	{
		return authenticationReadRecord;
	}

	public IORecord getCredentialsReadRecord() 
	{
		return credentialsReadRecord;
	}

	public IORecord getReadyReadRecord() 
	{
		return readyReadRecord;
	}

	public void setAuthenticationReadRecord(IORecord authenticationReadRecord) 
	{
		this.authenticationReadRecord = authenticationReadRecord;
		outOfSync("authenticationReadRecord");
	}

	public void setCredentialsReadRecord(IORecord credentialsReadRecord) 
	{
		this.credentialsReadRecord = credentialsReadRecord;
		outOfSync("credentialsReadRecord");
	}

	public void setReadyReadRecord(IORecord readyReadRecord) 
	{
		this.readyReadRecord = readyReadRecord;
		outOfSync("readyReadRecord");
	}

	public IORecord getStartupWriteRecord() {
		return startupWriteRecord;
	}

	public void setStartupWriteRecord(IORecord startupWriteRecord) {
		this.startupWriteRecord = startupWriteRecord;
		outOfSync("startupWriteRecord");
	}

	public IORecord getStartupReadRecord() {
		return startupReadRecord;
	}

	public void setStartupReadRecord(IORecord startupReadRecord) {
		this.startupReadRecord = startupReadRecord;
		outOfSync("startupReadRecord");
	}

	public IORecord getCredentialsWriteRecord() 
	{
		return credentialsWriteRecord;
	}

	public IORecord getReadyWriteRecord() 
	{
		return readyWriteRecord;
	}

	public void setCredentialsWriteRecord(IORecord credentialsWriteRecord) 
	{
		this.credentialsWriteRecord = credentialsWriteRecord;
		outOfSync("credentialsWriteRecord");
	}

	public void setReadyWriteRecord(IORecord readyWriteRecord) 
	{
		this.readyWriteRecord = readyWriteRecord;
		outOfSync("readyWriteRecord");
	}

	public PoaState getPoaState() {
		return poaState;
	}

	public void setPoaState(PoaState connectioState) {
		this.poaState = connectioState;
		outOfSync("State");
	}
	
	public IORecord getReadyConfirmedWriteRecord() {
		return readyConfirmedWriteRecord;
	}

	public void setReadyConfirmedWriteRecord(IORecord readyConfirmedWriteRecord) {
		this.readyConfirmedWriteRecord = readyConfirmedWriteRecord;
		outOfSync("readyConfirmedWriteRecord");
	}

	public IORecord getReadyConfirmedReadRecord() {
		return readyConfirmedReadRecord;
	}

	public void setReadyConfirmedReadRecord(IORecord readyConfirmedReadRecord) {
		this.readyConfirmedReadRecord = readyConfirmedReadRecord;
		outOfSync("readyConfirmedReadRecord");
	}

	public NetworkRecord getReadyConfirmedNetworkRecord() {
		return readyConfirmedNetworkRecord;
	}

	public void setReadyConfirmedNetworkRecord(NetworkRecord readyConfirmedNetworkRecord) {
		this.readyConfirmedNetworkRecord = readyConfirmedNetworkRecord;
		outOfSync("readyConfirmedNetworkRecord");
	}

	@Override
	public String localRepository() 
	{
		return String.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.auth_location"))+getProducerPortNumber()+"/";
	}
	
	public Integer getMessageDeliveryState() {
		return messageDeliveryState;
	}

	public void setMessageDeliveryState(Integer messageDeliveryState) 
	{
		this.messageDeliveryState = messageDeliveryState;
		outOfSync("messageDeliveryState");
	}
	
	public static class Builder
	{
		private String messageId;

		private long createdOn;
		
		private Integer originatingPort;

		public Builder messageId(String messageId)
		{
			this.messageId = messageId;
			return this;
		}

		public Builder createdOn(long createdOn)
		{
			this.createdOn = createdOn;
			return this;
		}

		public Builder originatingPort(Integer originatingPort)
		{
			this.originatingPort = originatingPort;
			return this;
		}
		
		public ProofOfAuthentication build()
		{
			return new ProofOfAuthentication(messageId, this.createdOn == 0L? new Date().getTime() : this.createdOn, originatingPort);
		}
	}

	@Override
	public void saveInCache() 
	{
		DocumentCache.getInstance()
			.getAuthenticationMessageCache()
			.put(getProducerPortNumber(), this);
	}

	public static ProofOfAuthentication load(Integer originatingPort, String docName)
	{
		try
		{
			return DocumentCache.getInstance().getAuthenticationMessageCache().getDocument(originatingPort, docName);
		} catch (NullPointerException e){return null;}
	}

	@Override
	public void removeFromCache()
	{
		DocumentCache.getInstance()
			.getAuthenticationMessageCache()
			.removeFromCache(getProducerPortNumber(), this);
	}
	
	@Override
	/**
	 * As of now POA are not persisted in the database. They only reside in the cache
	 */
	public void saveInDB() {}
}
