package com.ics.synapse.worker;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.nceph.core.receptor.exception.ReceptorInstantiationException;
import com.ics.nceph.core.worker.Reader;
import com.ics.synapse.message.type.SynapticIncomingMessageType;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public class SynapticReader extends Reader
{
	public SynapticReader(Connection connection, Message message)
	{
		super(connection, message);
	}

	@Override
	public void execute()  
	{
		try 
		{
			// 1. Classload the Receptor class for the message type
			Receptor receptor = new Receptor.Builder()
					.message(getMessage())
					.incomingConnection(getConnection())
					.implementationClass(SynapticIncomingMessageType.getMessageType(getMessage().getType()).getProcessorClass())
					.build();
			
			// 2. Process the message by calling the process of Receptor
			receptor.execute();
		} 
		catch (InvalidMessageTypeException e) {
			//LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("Invalid message type")
					.logError(),e);
		} 
		catch (ReceptorInstantiationException e) 
		{
			//LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("Receptor can't initialize")
					.logError(),e);
		}
	}
}
