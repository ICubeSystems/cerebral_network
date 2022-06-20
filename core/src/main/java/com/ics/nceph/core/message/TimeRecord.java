package com.ics.nceph.core.message;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 15-Mar-2022
 */
public class TimeRecord 
{
	private long start;
	
	private long end;
	
	TimeRecord() {}
	
	TimeRecord(long start, long end)
	{
		this.start = start;
		this.end = end;
	}
	
	public long getStart() {
		return start;
	}
	
	public long getEnd() {
		return end;
	}
}
