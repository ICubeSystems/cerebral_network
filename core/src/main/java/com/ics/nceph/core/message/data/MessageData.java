package com.ics.nceph.core.message.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 29-Mar-2022
 */
public abstract class MessageData 
{
	Date createdOn;
	
	public MessageData() 
	{
		createdOn = new Date();
	}

	public Date getCreatedOn() 
	{
		return createdOn;
	}
	
	public byte[] bytes() throws MessageBuildFailedException 
	{
		try
		{
			ObjectMapper mapper = new ObjectMapper();
			String JSON = mapper.writeValueAsString(this);
			return JSON.getBytes(StandardCharsets.UTF_8);
		} catch (IOException e)
		{
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.action(this.getClass().getSimpleName() + " build failed")
					.logError(), e);
			//throw MessageBuildFailedException
			throw new MessageBuildFailedException(this.getClass().getSimpleName() + " build failed", e);
		}
	}
}
