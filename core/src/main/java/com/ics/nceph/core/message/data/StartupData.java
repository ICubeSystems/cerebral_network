package com.ics.nceph.core.message.data;

import java.io.Serializable;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 29-Mar-2022
 */
public class StartupData extends MessageData implements Serializable 
{
	private static final long serialVersionUID = 1L;

	public StartupData() {}

	public StartupData(long startupNetworkRecord) 
	{
		super();
	}


	public static class Builder
	{
		public StartupData build() 
		{
			return new StartupData();
		}
	}
}
