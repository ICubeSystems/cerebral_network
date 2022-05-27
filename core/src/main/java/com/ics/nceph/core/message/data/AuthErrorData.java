package com.ics.nceph.core.message.data;

import java.io.Serializable;
import java.util.Date;
import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 09-Apr-2022
 */
public class AuthErrorData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;

	private NetworkRecord credentialsNetworkRecord;

	private Date authenticationErrorNetworkRecord;

	private IORecord credentialsReadRecord;

	private IORecord authenticationWriteRecord;

	public AuthErrorData() {}

	public AuthErrorData(NetworkRecord credentialsNetworkRecord, Date authenticationErrorNetworkRecord, IORecord credentialsReadRecord, IORecord authenticationWriteRecord) 
	{
		this.credentialsNetworkRecord = credentialsNetworkRecord;
		this.authenticationErrorNetworkRecord = authenticationErrorNetworkRecord;
		this.credentialsReadRecord = credentialsReadRecord;
		this.authenticationWriteRecord = authenticationWriteRecord;
	}

	public NetworkRecord getCredentialsNetworkRecord() 
	{
		return credentialsNetworkRecord;
	}

	public Date getAuthenticationErrorNetworkRecord() 
	{
		return authenticationErrorNetworkRecord;
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

		private Date authenticationErrorNetworkRecord;

		public Builder credentialsNetworkRecord(NetworkRecord CredentialsNetworkRecord)
		{
			this.credentialsNetworkRecord = CredentialsNetworkRecord;
			return this;
		}

		public Builder credentialsReadRecord(IORecord credentialsReadRecord)
		{
			this.credentialsReadRecord = credentialsReadRecord;
			return this;
		}

		public Builder authenticationWriteRecord(IORecord authenticationWriteRecord)
		{
			this.authenticationWriteRecord = authenticationWriteRecord;;
			return this;
		}
		public Builder authenticationErrorNetworkRecord(Date authenticationErrorNetworkRecord)
		{
			this.authenticationErrorNetworkRecord = authenticationErrorNetworkRecord;
			return this;
		}

		public AuthErrorData build()
		{
			return new AuthErrorData(credentialsNetworkRecord, authenticationErrorNetworkRecord, credentialsReadRecord, authenticationWriteRecord);
		}
	}
}
