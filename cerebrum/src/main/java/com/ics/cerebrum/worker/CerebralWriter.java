package com.ics.cerebrum.worker;

import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.affector.AffectorInstantiationException;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.exception.InvalidMessageTypeException;
import com.ics.nceph.core.worker.Writer;

/**
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public class CerebralWriter extends Writer 
{
	public CerebralWriter(Connection connection, Message message)
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
					.implementationClass(CerebralOutgoingMessageType.getMessageType(getMessage().getType()).getAffectorClass())
					.build();
			
			// 2. Process the message by calling the process of Affector
			affector.execute();
		} 
		catch (InvalidMessageTypeException e) 
		{
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
