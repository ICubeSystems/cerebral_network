package com.ics.nceph.core.message;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Getter;
import lombok.Setter;

/**
 * This class records the time taken in the IO operation involved like<br>
 * <ol>
 * 	<li>Reading a message from the socket channel</li>
 * 	<li>Writing a message on to a socket channel</li>
 * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 15-Mar-2022
 */
@Getter
@Setter
@DynamoDBDocument
public class IORecord 
{
	@DynamoDBAttribute
	private long start;
	
	@DynamoDBAttribute
	private long end;
	
	public IORecord() {}
	
	public IORecord(long start, long end) 
	{
		this.start = start;
		this.end = end;
	}
	
	/**
	 * 
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 15-Mar-2022
	 */
	public static class Builder
	{
		private long start;
		
		private long end;
		
		public Builder start(long start)
		{
			this.start = start;
			return this;
		}
		
		public Builder end(long end)
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
