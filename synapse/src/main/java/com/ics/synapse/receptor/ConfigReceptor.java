package com.ics.synapse.receptor;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.id.exception.IdGenerationFailedException;
import com.ics.logger.BootstraperLog;
import com.ics.logger.ConnectionLog;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.AuthenticationFailedException;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.connection.exception.ConnectionInitializationException;
import com.ics.nceph.core.connector.state.ConnectorState;
import com.ics.nceph.core.db.document.exception.CacheInitializationException;
import com.ics.nceph.core.db.document.store.ConfigStore;
import com.ics.nceph.core.db.document.store.IdStore;
import com.ics.nceph.core.db.document.store.cache.DocumentCache;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.ConfigData;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.synapse.connector.SynapticConnector;
import com.ics.synapse.db.document.cache.SynapseCacheInitializer;
import com.ics.synapse.message.type.SynapticIncomingMessageType;

/**
 * This {@link Receptor} class is invoked when synapse receives a {@link SynapticIncomingMessageType#CONFIG} message. <br>
 * The incoming messages is processed as follows:
 * <ul>
 * 	<li> {@link ConfigData} received from cerebrum for this synaptic application </li>
 * 	<li> If {@link ConfigData} have error message then set connector state {@link ConnectorState#AUTH_FAILED} </li>
 * 	<li> If {@link ConfigData} doesn't have any error then configure synapse as follows:
 * 		<ul>
 * 			<li> Initiate {@link IdStore} and set {@link ConfigData#getMessageCount()} to {@link IdStore} </li>
 * 			<li> Initiate {@link ConfigStore} with {@link ConfigData} which sets node id and list of application receptors to {@link ConfigStore} </li>
 * 			<li> Initiate {@link DocumentCache} </li>
 * 			<li> At last initiate connections using {@link SynapticConnector#initiateConnections()} and then sets connector state to {@link ConnectorState#READY Ready} </li>
 * 		</ul>	
 * 	</li>		
 * </ul>
 * <br>
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
			//LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.description("Class Name: " + this.getClass().getSimpleName())
					.action("ConfigData mapping failed")
					.logError(),e);
		}
	}


	@Override
	public void process() 
	{
		try
		{
			SynapticConnector connector = (SynapticConnector)getIncomingConnection().getConnector();
			if(configData.getError() == null) 
			{
				// Initiate {@link IdStore} and set {@link ConfigData#getMessageCount()} to {@link IdStore}
				IdStore.getInstance().initialize();
				// Set message count received from cerebrum if application's message count is null or less than received one.
				IdStore.getInstance().compareAndSet(configData.getMessageCount(), 100);
				// Initialize the ConfigStore by passing the config data received by cerebrum 
				ConfigStore.getInstance().init(configData);
				//Initiate {@link DocumentCache}
				DocumentCache.initialize();
				// Initialize cache
				SynapseCacheInitializer.run();
				// Initialize connections after control connection authentication.
				connector.initiateConnections();
				connector.setState(ConnectorState.READY);
				return;
			}
			connector.setState(ConnectorState.AUTH_FAILED);

		} catch (ConnectionInitializationException | ConnectionException | AuthenticationFailedException e) 
		{
			// Log
			NcephLogger.CONNECTION_LOGGER.fatal(new ConnectionLog.Builder()
					.action("Connection failed")
					.logError(),e);
		} catch (CacheInitializationException e)
		{
			// Log
			NcephLogger.BOOTSTRAP_LOGGER.fatal(new BootstraperLog.Builder()
					.action("Document cache initialization failed")
					.logError(),e);
		}
		catch(IdGenerationFailedException e){
			// Log
			NcephLogger.BOOTSTRAP_LOGGER.fatal(new BootstraperLog.Builder()
					.action("Id store initialization failed")
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
