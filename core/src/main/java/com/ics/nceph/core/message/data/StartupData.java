package com.ics.nceph.core.message.data;

import java.io.Serializable;
import java.util.Date;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 29-Mar-2022
 */
public class StartupData extends MessageData implements Serializable 
{
	private static final long serialVersionUID = 1L;

	private Date startupNetworkRecord;

	public StartupData() {}

	public StartupData(Date startupNetworkRecord) 
	{
		super();
		this.startupNetworkRecord = startupNetworkRecord;
	}

	public Date getStartupNetworkRecord() 
	{
		return startupNetworkRecord;
	}

	public static class Builder
	{
		private Date startupNetworkRecord;

		public Builder startupNetworkRecord(Date startupNetworkRecord)
		{
			this.startupNetworkRecord = startupNetworkRecord;
			return this;
		}

		public StartupData build() 
		{
			return new StartupData(startupNetworkRecord);
		}
	}
}
