package com.ics.synapse.worker;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.affector.AffectorInstantiationException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.worker.Writer;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public class SynapticWriter extends Writer 
{
	public SynapticWriter(Connection connection ,Message message)
	{
		super(connection, message);
	}

	@Override
	public void execute() 
	{
		try 
		{
			// 1. Classload the Affector class for the message type
			Affector affector = new Affector.Builder()
					.message(getMessage())
					.incomingConnection(getConnection())
					.implementationClass(SynapticOutgoingMessageType.getMessageType(getMessage().getType()).getAffectorClass())
					.build();
			
			// 2. Process the message by calling the process of Affector
			affector.process();
		} 
		catch (InvalidMessageTypeException e) 
		{
			e.printStackTrace();
		} 
		catch (AffectorInstantiationException e) 
		{
			e.printStackTrace();
		}
	}
}