package com.ics.nceph.core.message;

import java.util.Date;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 15-Mar-2022
 */
public class TimeRecord 
{
	private Date start;
	
	private Date end;
	
	TimeRecord(Date start, Date end)
	{
		this.start = start;
		this.end = end;
	}
	
	public Date getStart() {
		return start;
	}
	
	public Date getEnd() {
		return end;
	}
}
