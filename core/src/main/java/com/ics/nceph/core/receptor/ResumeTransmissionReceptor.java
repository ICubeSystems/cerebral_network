package com.ics.nceph.core.receptor;

import com.ics.logger.BackpressureLog;
import com.ics.logger.LogData;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/** 
 * @author Anshul
 * @since 21-Nov-2022
 */
public class ResumeTransmissionReceptor extends Receptor 
{
	public ResumeTransmissionReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		NcephLogger.BACKPRESSURE_LOGGER.info(new BackpressureLog.Builder()
				.nodeId(String.valueOf(incomingConnection.getNodeId()))
				.action("Resume transmission")
				.data(new LogData()
						.entry("Port", String.valueOf(incomingConnection.getConnector().getPort()))
						.entry("ConnectionId", String.valueOf(incomingConnection.getId()))
						.toString())
				.logInfo());
			getIncomingConnection().getConnector().resumeTransmission(getIncomingConnection().getNodeId());
	}
}
