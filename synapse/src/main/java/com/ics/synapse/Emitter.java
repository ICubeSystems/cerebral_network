package com.ics.synapse;

import java.nio.channels.SelectionKey;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.message.Message;
import com.ics.synapse.connector.SynapticConnector;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Jan-2022
 */
public final class Emitter 
{
	private static SynapticConnector connector;
	
	private Emitter()
	{
	}
	
	public static void initiate(SynapticConnector connector)
	{
		if (Emitter.connector == null)
			Emitter.connector = connector;
	}
	
	public static synchronized void emit(Message message) throws ImproperConnectorInstantiationException
	{
		Connection connection = connector.getConnection();
		connection.enqueueMessage(message);
		connection.setInterest(SelectionKey.OP_WRITE);
	}
}
