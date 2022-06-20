package com.ics.nceph.core.affector;

import java.lang.reflect.InvocationTargetException;

import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.type.MessageType;
import com.ics.nceph.core.worker.Writer;

/**
 * This class is responsible for processing the outgoing message inside a writer (worker) thread after the message has been sent over the socket channel.
 * {@link Connection#write()} writes the {@link Message} and then starts a {@link Writer} thread to post process the sent message. 
 * The {@link Writer} instantiates <b>Affector</b> based on MessageType
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public abstract class Affector 
{
	Message message;
	
	Connection incomingConnection;
	
	/**
	 * Abstract method to be implemented by all the Affector classes
	 * 
	 * @return void
	 */
	abstract public void process();
	
	public void execute()
	{
		// Log
		NcephLogger.MESSAGE_LOGGER.info(
				new MessageLog.Builder()
				.messageId(getMessage().decoder().getId())
				.action("RECEIVED")
				.data(new LogData()
						.entry("messageType", MessageType.getNameByType(getMessage().decoder().getType()))
						.entry("dataBytes", String.valueOf(getMessage().decoder().getDataLength()))
						.toString())
				.logInfo());
		process();
	}
	
	public Affector(Message message, Connection incomingConnection)
	{
		this.message = message;
		this.incomingConnection = incomingConnection;
	}
	
	public Message getMessage() {
		return message;
	}

	public Connection getIncomingConnection() {
		return incomingConnection;
	}
	
	public static class Builder
	{
		Message message;
		
		Connection incomingConnection;
		
		Class<? extends Affector> implementationClass;
		
		public Builder message(Message message) {
			this.message = message;
			return this;
		}
		
		public Builder incomingConnection(Connection incomingConnection) {
			this.incomingConnection = incomingConnection;
			return this;
		}
		
		public Builder implementationClass(Class<? extends Affector> implementationClass) {
			this.implementationClass = implementationClass;
			return this;
		}
		
		public Affector build() throws AffectorInstantiationException
		{
			try 
			{
				// Class load the Receptor object
				Class<?>[] constructorParamTypes = {Message.class, Connection.class};
				Object[] params = {message, incomingConnection};
				
				// Return Receptor
				return implementationClass.getConstructor(constructorParamTypes).newInstance(params);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException	| InvocationTargetException | NoSuchMethodException | SecurityException e) 
			{
				throw new AffectorInstantiationException("Affector instantiation exception", e);
			}
		}
	}
}
