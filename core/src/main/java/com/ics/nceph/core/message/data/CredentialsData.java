package com.ics.nceph.core.message.data;

import java.io.Serializable;
import java.util.Date;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 04-Apr-2022
 */
public class CredentialsData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;

	private IORecord startupWriteRecord;

	private IORecord authenticationReadRecord;

	private NetworkRecord authenticationNetworkRecord;

	private Date credentialsNetworkRecord;

	private String credentials;

	public CredentialsData() {}

	public CredentialsData(NetworkRecord authenticationNetworkRecord, Date credentialsNetworkRecord, String credentials, IORecord startupWriteRecord, IORecord authenticationReadRecord) 
	{
		super();
		this.authenticationNetworkRecord = authenticationNetworkRecord;
		this.credentialsNetworkRecord = credentialsNetworkRecord;
		this.credentials = credentials;
		this.startupWriteRecord = startupWriteRecord;
		this.authenticationReadRecord = authenticationReadRecord;
	}

	public NetworkRecord getAuthenticationNetworkRecord() 
	{
		return authenticationNetworkRecord;
	}

	public Date getCredentialsNetworkRecord() 
	{
		return credentialsNetworkRecord;
	}

	public String getCredentials() 
	{
		return credentials;
	}

	public IORecord getStartupWriteRecord() 
	{
		return startupWriteRecord;
	}

	public IORecord getAuthenticationReadRecord() 
	{
		return authenticationReadRecord;
	}

	public static class Builder
	{
		private IORecord startupWriteRecord;

		private IORecord authenticationReadRecord;

		private NetworkRecord authenticationNetworkRecord;

		private Date credentialsNetworkRecord;

		private String credentials;

		public Builder startupWriteRecord(IORecord startupWriteRecord)
		{
			this.startupWriteRecord = startupWriteRecord;
			return this;
		}

		public Builder authenticationReadRecord(IORecord authenticationReadRecord)
		{
			this.authenticationReadRecord = authenticationReadRecord;
			return this;
		}

		public Builder authenticationNetworkRecord(NetworkRecord authenticationNetworkRecord)
		{
			this.authenticationNetworkRecord = authenticationNetworkRecord;
			return this;
		}

		public Builder credentialsNetworkRecord(Date credentialsNetworkRecord)
		{
			this.credentialsNetworkRecord = credentialsNetworkRecord;
			return this;
		}

		public Builder credentials(String credentials)
		{
			this.credentials = credentials;
			return this;
		}

		public CredentialsData build()
		{
			return new CredentialsData(authenticationNetworkRecord, credentialsNetworkRecord, credentials, startupWriteRecord, authenticationReadRecord);
		}
	}
}
