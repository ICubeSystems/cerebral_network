package com.ics.nceph.core.message.data;

import java.io.Serializable;

import com.ics.nceph.core.message.IORecord;
import com.ics.nceph.core.message.NetworkRecord;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 30-Mar-2022
 */
public class AuthenticationData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;

	private NetworkRecord startupNetworkRecord;

	private IORecord startupReadRecord;

	public AuthenticationData() {}

	public AuthenticationData(NetworkRecord startupNetworkRecord, IORecord startupReadRecord) 
	{
		super();
		this.startupReadRecord = startupReadRecord;
		this.startupNetworkRecord = startupNetworkRecord;
	}

	public IORecord getStartupReadRecord() 
	{
		return startupReadRecord;
	}

	public NetworkRecord getStartupNetworkRecord() 
	{
		return startupNetworkRecord;
	}

	public static class Builder
	{
		private NetworkRecord startupNetworkRecord;

		private IORecord startupReadRecord;

		public Builder startupNetworkRecord(NetworkRecord startupNetworkRecord)
		{
			this.startupNetworkRecord = startupNetworkRecord;
			return this;
		}

		public Builder startupReadRecord(IORecord readRecord)
		{
			this.startupReadRecord = readRecord;
			return this;
		}
		public AuthenticationData build()
		{
			return new AuthenticationData(startupNetworkRecord, startupReadRecord);
		}
	}
}
