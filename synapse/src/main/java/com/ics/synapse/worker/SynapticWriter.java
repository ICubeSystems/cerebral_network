package com.ics.synapse.worker;

import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
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
			affector.execute();
		} 
		catch (InvalidMessageTypeException e) {
			//LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("Invalid message type")
					.logError(),e);
		} 
		catch (AffectorInstantiationException e) 
		{
			//LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("Affector can't initialize")
					.logError(),e);
		}
	}
}