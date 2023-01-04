package com.ics.nceph.core.worker;

import java.util.concurrent.ThreadPoolExecutor;

import com.ics.logger.BackpressureLog;
import com.ics.logger.LogData;
import com.ics.logger.NcephLogger;

/**
 * A handler for {@link Reader} task that cannot be executed by a {@link WorkerPool}.
 * 
 * @author Anurag Arya, Anshul Arya
 * @version 1.0
 * @since 23-Dec-2021
 */
public class RejectedReaderHandler extends RejectedWorkerHandler<Reader>
{
	@Override
	public void rejectedExecution(Runnable worker, ThreadPoolExecutor workerPool) 
	{
		super.rejectedExecution(worker, workerPool);
		
		// Check backpressure enabled or not
		if (!getPool().isBackPressureInitiated())
		{
			// Set backpressure to true
			getPool().setBackPressureInitiated(true,(Worker)worker);
			
			// Send pause message to producer
			getConnection().getConnector().signalPauseTransmission(connection);
			
			// Log
			NcephLogger.BACKPRESSURE_LOGGER.info(new BackpressureLog.Builder()
					.nodeId(String.valueOf(connection.getNodeId()))
					.action("Signal Pause transmission")
					.data(new LogData()
							.entry("Port", String.valueOf(connection.getConnector().getPort()))
							.entry("ConnectionId", String.valueOf(connection.getId()))
							.toString())
					.logInfo());
		}
		
		getPool().getRejectedWorkerQueue().add(worker);
	}
}
