package com.ics.logger;

import java.util.ArrayList;
import java.util.List;

public class LogData
{
	List<String> data;
	
	public LogData()
	{
		data = new ArrayList<String>();
	}
	
	public LogData entry(String key, String value)
	{
		data.add(key.toString()+": "+value.toString());
		return this;
	}
	
	public String toString()
	{
		return String.join(", ",data);
	}
}
