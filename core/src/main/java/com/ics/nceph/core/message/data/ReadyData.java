package com.ics.nceph.core.message.data;

import java.io.Serializable;

import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 08-Apr-2022
 */
public class ReadyData extends MessageData implements Serializable 
{
	private static final long serialVersionUID = 1L;

	private NetworkRecord credentialsNetworkRecord;

	private IORecord credentialsReadRecord;

	private IORecord authenticationWriteRecord;

	private long readyNetworkRecord;

	public ReadyData() {}

	public ReadyData(NetworkRecord credentialsNetworkRecord, IORecord credentialsReadRecord, IORecord authenticationWriteRecord, long readyNetworkRecord)  
	{
		this.credentialsNetworkRecord = credentialsNetworkRecord;
		this.credentialsReadRecord = credentialsReadRecord;
		this.authenticationWriteRecord = authenticationWriteRecord;
		this.readyNetworkRecord = readyNetworkRecord;
	}

	public NetworkRecord getCredentialsNetworkRecord() 
	{
		return credentialsNetworkRecord;
	}

	public long getReadyNetworkRecord() 
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

	public static class Builder
	{
		private NetworkRecord credentialsNetworkRecord;

		private IORecord credentialsReadRecord;

		private IORecord authenticationWriteRecord;

		private long readyNetworkRecord;

		public Builder credentialsNetworkRecord(NetworkRecord credentialsNetworkRecord)
		{
			this.credentialsNetworkRecord = credentialsNetworkRecord;
			return this;
		}

		public Builder credentialsReadRecord(IORecord credentialsReadRecord)
		{
			this.credentialsReadRecord = credentialsReadRecord;
			return this;
		}

		public Builder readyNetworkRecord(long readyNetworkRecord)
		{
			this.readyNetworkRecord = readyNetworkRecord;
			return this;
		}

		public Builder authenticationWriteRecord(IORecord authenticationWriteRecord)
		{
			this.authenticationWriteRecord = authenticationWriteRecord;
			return this;
		}

		public ReadyData build()
		{
			return new ReadyData(credentialsNetworkRecord, credentialsReadRecord, authenticationWriteRecord, readyNetworkRecord);
		}
	}
}
