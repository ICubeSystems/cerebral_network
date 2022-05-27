package com.ics.nceph.core.message.data;

import java.io.Serializable;
import java.util.Date;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 18-May-2022
 */
public class ReadyConfirmedData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;

	private NetworkRecord credentialsNetworkRecord;

	private IORecord credentialsWriteRecord;

	private IORecord credentialsReadRecord;

	private IORecord readyReadRecord;

	private IORecord authenticationWriteRecord;

	private NetworkRecord readyNetworkRecord;

	private Date readyConfirmedNetwork;

	public ReadyConfirmedData() {}

	public ReadyConfirmedData(IORecord credentialsWriteRecord, IORecord readyReadRecord, NetworkRecord readyNetworkRecord, Date readyConfirmedNetwork)  
	{
		this.credentialsWriteRecord = credentialsWriteRecord;
		this.readyReadRecord = readyReadRecord;
		this.readyNetworkRecord = readyNetworkRecord;
		this.readyConfirmedNetwork = readyConfirmedNetwork;
	}

	public NetworkRecord getCredentialsNetworkRecord() 
	{
		return credentialsNetworkRecord;
	}

	public NetworkRecord getReadyNetworkRecord() 
	{
		return readyNetworkRecord;
	}

	public IORecord getCredentialsReadRecord() 
	{
		return credentialsReadRecord;
	}

	public IORecord getAuthenticationWriteRecord() 
	{
		return authenticationWriteRecord;
	}

	public IORecord getCredentialsWriteRecord() 
	{
		return credentialsWriteRecord;
	}

	public IORecord getReadyReadRecord() 
	{
		return readyReadRecord;
	}
	
	public Date getReadyConfirmedNetwork() {
		return readyConfirmedNetwork;
	}

	public static class Builder
	{
		private IORecord credentialsWriteRecord;

		private IORecord readyReadRecord;

		private NetworkRecord readyNetworkRecord;

		private Date readyConfirmedNetwork;

		public Builder credentialsWriteRecord(IORecord credentialsWriteRecord)
		{
			this.credentialsWriteRecord = credentialsWriteRecord;
			return this;
		}

		public Builder readyReadRecord(IORecord readyReadRecord)
		{
			this.readyReadRecord = readyReadRecord;
			return this;
		}

		public Builder readyNetworkRecord(NetworkRecord readyNetworkRecord)
		{
			this.readyNetworkRecord = readyNetworkRecord;
			return this;
		}
		
		public Builder readyConfirmedNetwork(Date readyConfirmedNetwork)
		{
			this.readyConfirmedNetwork = readyConfirmedNetwork;
			return this;

		}
		public ReadyConfirmedData build()
		{
			return new ReadyConfirmedData(credentialsWriteRecord, readyReadRecord, readyNetworkRecord, readyConfirmedNetwork);
		}
	}
}
