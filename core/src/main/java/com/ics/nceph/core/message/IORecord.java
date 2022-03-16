package com.ics.nceph.core.message;

import java.util.Date;

/**
 * This class records the time taken in the IO operation involved like:
 * 1. Reading a message from the socket channel
 * 2. Writing a message on to a socket channel
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 15-Mar-2022
 */
public class IORecord extends TimeRecord 
{

	private IORecord(Date start, Date end) 
	{
		super(start, end);
	}
	
	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 15-Mar-2022
	 */
	public static class Builder
	{
		private Date start;
		
		private Date end;
		
		public Builder start(Date start)
		{
			this.start = start;
			return this;
		}
		
		public Builder end(Date end)
		{
			this.end = end;
			return this;
		}
	
		public IORecord build() 
		{
			return new IORecord(start, end);
		}
	}
}
