package com.ics.nceph.core.message.data;

import java.io.Serializable;

/**
 * 
 * @author Anshul
 * @since 22-Nov-2022
 */
public class BackpressureData extends MessageData implements Serializable
{
	private static final long serialVersionUID = 1L;

	public BackpressureData() {}

	public static class Builder
	{
		public BackpressureData build() 
		{
			return new BackpressureData();
		}
	}
}
