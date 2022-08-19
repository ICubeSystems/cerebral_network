package com.ics.cerebrum.receptor;

import java.nio.channels.SelectionKey;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.cerebrum.mac.SynapticMappingStore;
import com.ics.cerebrum.message.ConfigMessage;
import com.ics.logger.MessageLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.config.exception.NodeResolutionException;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.QueuingContext;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.data.BootstrapData;
import com.ics.nceph.core.message.data.ConfigData;
import com.ics.nceph.core.message.exception.MessageBuildFailedException;
import com.ics.nceph.core.receptor.Receptor;

/**
 * Receptor class to receive {@link BootstrapMessage} from synapse. 
 * 
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
			ConfigMessage message = null;
			// 1. Do the node resolution using the MAC address from the bootstrapData
			Integer nodeId = SynapticMappingStore.resolveNode(bootstrapData.getMacAddress() + "-" + getIncomingConnection().getConnector().getPort());
			// 2. Create ConfigMessage
			message = new ConfigMessage.Builder()
					.data(new ConfigData.Builder()
							.nodeId(nodeId)
							.eventReceptors(ConnectorCluster.applicationReceptors.get(getIncomingConnection().getConnector().getPort()))
							.build())
					.mid(getMessage().decoder().getId())
					.build();
			
			// 3. Send the config message back to the synaptic node
			getIncomingConnection().enqueueMessage(message, QueuingContext.QUEUED_FROM_RECEPTOR);
			getIncomingConnection().setInterest(SelectionKey.OP_WRITE);
		} 
		catch (NodeResolutionException e) {
			// Log
			NcephLogger.MESSAGE_LOGGER.fatal(new MessageLog.Builder()
					.messageId(getMessage().decoder().getId())
					.action("Node resolution failed")
					.logError(),e);
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
