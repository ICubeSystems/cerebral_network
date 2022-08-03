package com.ics.synapse.receptor;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.logger.ConnectionLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.config.ConfigStore;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.AuthenticationFailedException;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ConfigData;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.connector.SynapticConnector;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since July 28, 2022
 */
public class ConfigReceptor extends Receptor 
{
	private ConfigData configData;
	
	public ConfigReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try {
			configData = (ConfigData)message.decoder().getData(ConfigData.class);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	@Override
	public void process() 
	{
		try
		{
			// Initialize the ConfigStore by passing the config data received by cerebrum 
			ConfigStore.init(configData);
			
			SynapticConnector connector = (SynapticConnector)getIncomingConnection().getConnector();
			// Initialize connections after control connection authentication.
			connector.initiateConnections();
		}catch (ConnectionInitializationException | ConnectionException | AuthenticationFailedException e) 
		{
			// Log
			NcephLogger.CONNECTION_LOGGER.fatal(new ConnectionLog.Builder()
					.action("Connection failed")
					.logError(),e);
		}
		finally 
		{
			// Teardown the control connection
			try {
				getIncomingConnection().teardown();
			} catch (IOException e) {
				NcephLogger.CONNECTION_LOGGER.error(new ConnectionLog.Builder()
						.connectionId(String.valueOf(getIncomingConnection().getId()))
						.action("requesting teardown failed")
						.logError(), e);
			}
		}
	}
}
