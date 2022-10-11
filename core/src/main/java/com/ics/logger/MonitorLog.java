package com.ics.logger;

import java.util.Random;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 06-Apr-2022
 */
public class MonitorLog
{
	public static class Builder
	{
		Integer monitorPort;
		
		String action;
		
		String data;
		
		String description;
		
		Exception exception;
		
		StringBuilder logString = new StringBuilder();
		
		public Builder monitorPort(Integer monitorPort)
		{
			this.monitorPort = monitorPort;
			return this;
		}
		
		public Builder action(String action)
		{
			this.action = action.toUpperCase();
			return this;
		}
		
		public Builder data(String data)
		{
			this.data = data;
			return this;
		}
		
		public Builder description(String description)
		{
			this.description = description;
			return this;
		}
		
		public Builder exception(Exception exception)
		{
			this.exception = exception;
			return this;
		}
		
		private String buildLogMessage()
		{
			return new StringBuilder().append(monitorPort != null ? "[" + monitorPort + "]" : "")
					.append(action != null ? "[" + action + "]" : "")
					.append(data != null ? "[" + data + "]" : "")
					.append(description != null ? "[" + description + "]" : "")
					.toString();
		}
		
		public String logInfo()
		{
			return buildLogMessage();
		}
		
		public String logError()
		{
			String errorId = String.valueOf(new Random().nextLong());
			return buildLogMessage() + "[errorLogId: "+ errorId + "]";
		}
	}
	
}
