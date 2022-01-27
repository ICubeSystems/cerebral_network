package com.ics.nceph.core.connector.exception;

public class ImproperMonitorInstantiationException extends Exception
{
	private static final long serialVersionUID = -2532031462951400280L;

	public ImproperMonitorInstantiationException(Exception e) 
	{
		e.printStackTrace();
	}
}
