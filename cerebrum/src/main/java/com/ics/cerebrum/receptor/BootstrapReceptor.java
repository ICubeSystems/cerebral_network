package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.cerebrum.configuration.CerebralConfiguration;
import com.ics.cerebrum.configuration.exception.ConfigurationException;
import com.ics.cerebrum.message.ConfigMessage;
import com.ics.cerebrum.message.type.CerebralIncomingMessageType;
import com.ics.cerebrum.message.type.CerebralOutgoingMessageType;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.BootstrapData;
import com.ics.nceph.core.message.data.ConfigData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.Receptor;
import com.ics.util.ApplicationContextUtils;

/**
 * This {@link Receptor} class is invoked when cerebrum receives a {@link CerebralIncomingMessageType#BOOTSTRAP} message. <br>
 * The incoming messages is processed as follows:
 * <ul>
 * 	<li> {@link BootstrapData} received from synapse, Which contains a secret key. Using that secret key cerebrum find node id for synaptic application</li>
 * 	<li> If cerebrum can't find node id for that secret key cerebrum builds {@link ConfigData} with error message. </li>
 * 	<li> If cerebrum find node id for that secret key then it builds {@link ConfigData} with following attributes:
 * 		<ul>
 * 			<li> Node id for synaptic application </li>
 * 			<li> Count of messages received from this application </li>
 * 			<li> List of application receptor according to event type </li>
 * 		</ul>	
 * 	</li>
 *  <li> Send {@link CerebralOutgoingMessageType#CONFIG Configuration} message with {@link ConfigData} to synaptic application. </li>	
 * </ul>
 * <br>
 * @author Anshul
 * @version 1.0
 * @since 28-Jul-2022
 */
public class BootstrapReceptor extends Receptor 
{
	private BootstrapData bootstrapData;

	public BootstrapReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
		try {
			bootstrapData = (BootstrapData)message.decoder().getData(BootstrapData.class);
		} catch (JsonProcessingException e) 
		{
			// LOG
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.description("Class Name: " + this.getClass().getSimpleName())
					.action("Bootstrap data mapping failed")
					.logError(),e);
		}
	}

	@Override
	public void process() 
	{
		try 
		{
			// 1. Do the node resolution using the MAC address from the bootstrapData
			Integer nodeId = null;
			try
			{
				CerebralConfiguration config = ApplicationContextUtils.context.getBean("cerebralConfiguration", CerebralConfiguration.class);
				nodeId = config.getNodeIdForKey(bootstrapData.getSecretKey());
			} 
			catch (ConfigurationException e)
			{
				NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
						.messageId(getMessage().decoder().getId())
						.action("Node resolution failed")
						.logError(),e);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			// 2. Create ConfigMessage
			ConfigData.Builder configDataBuilder = new ConfigData.Builder();
			if(nodeId == null)
				configDataBuilder.error("Your application is not registered");// TODO
			else 
			{
				configDataBuilder
				.nodeId(nodeId)
				.messageCount(getIncomingConnection().getConnector().getIncomingMessageRegister().messageCount(nodeId))
				.eventReceptors(ConnectorCluster.applicationReceptors.get(getIncomingConnection().getConnector().getPort()));
			}

			ConfigMessage message = new ConfigMessage.Builder()
					.data(configDataBuilder.build())
					.mid(getMessage().decoder().getId())
					.originatingPort(getMessage().decoder().getOriginatingPort())
					.build();

			// 3. Send the config message back to the synaptic node
			getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		}
		catch (MessageBuildFailedException e) {
			// Log
			NcephLogger.MESSAGE_LOGGER.error(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("CONFIG message build failed")
					.logError(),e);
		}
	}
}
