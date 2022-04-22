package com.ics.nceph.core.receptor;

import java.lang.reflect.InvocationTargetException;

import com.ics.logger.LogData;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.type.MessageType;
import com.ics.nceph.core.worker.Reader;

/**
 * This class is responsible for processing the incoming message inside a reader (worker) thread. 
 * {@link Connection#read()} reads the {@link Message} and then starts a {@link Reader} thread to process the incoming message. The {@link Reader} instantiates <b>Receptor</b> based on MessageType
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 05-Jan-2022
 */
public abstract class Receptor 
{
	Message message;
	
	Connection incomingConnection;
	
	/**
	 * Abstract method to be implemented by all the Receptor classes
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
						.entry("type", MessageType.getClassByType(getMessage().decoder().getType()))
						.entry("dataBytes", String.valueOf(getMessage().decoder().getDataLength()))
						.toString())
				.logInfo());
		process();
	}
	
	public Receptor(Message message, Connection incomingConnection)
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
		
		Class<? extends Receptor> implementationClass;
		
		public Builder message(Message message) {
			this.message = message;
			return this;
		}
		
		public Builder incomingConnection(Connection incomingConnection) {
			this.incomingConnection = incomingConnection;
			return this;
		}
		
		public Builder implementationClass(Class<? extends Receptor> implementationClass) {
			this.implementationClass = implementationClass;
			return this;
		}
		
		public Receptor build() throws ReceptorInstantiationException
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
				throw new ReceptorInstantiationException("Receptor instantiation exception", e);
			}
		}
	}
}
