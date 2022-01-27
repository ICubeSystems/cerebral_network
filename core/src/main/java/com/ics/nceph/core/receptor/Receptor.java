package com.ics.nceph.core.receptor;

import java.lang.reflect.InvocationTargetException;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
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
	 * 
	 * 
	 * @return void
	 */
	abstract public void process();
	
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
				throw new ReceptorInstantiationException(new Exception("Receptor instantiation exception"));
			}
		}
	}
}
