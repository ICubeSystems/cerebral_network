package com.ics.nceph.core.connector;

import java.nio.channels.SelectionKey;
import java.util.Map;
import java.util.function.Consumer;

import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.db.document.MessageDocument;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Oct 21, 2022
 */
public abstract class MonitorTask implements Consumer<Map.Entry<String, ProofOfPublish>>
{

	public void enqueueMessage(Connection connection, Message message) 
	{
		// 1. Enqueue the message on the connection to be sent to the Cerebrum
		connection.enqueueMessage(message, QueuingContext.QUEUED_FROM_MONITOR);
		// 2. Change the interest of the connection to write
		connection.setInterest(SelectionKey.OP_WRITE);
	}

	public boolean transmissionWindowElapsed(MessageDocument document) 
	{
		if (System.currentTimeMillis() - document.getCreatedOn() > Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("transmission.window"))  * 1000)
			return true;
		return false;
	}
	
}
