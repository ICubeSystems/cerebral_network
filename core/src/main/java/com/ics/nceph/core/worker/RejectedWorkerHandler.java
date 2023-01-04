package com.ics.nceph.core.worker;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

import lombok.Getter;

/**
 * 
 * @author Anshul
 * @since 22-Nov-2022
 *
 */
@Getter
public class RejectedWorkerHandler<T extends Worker> implements RejectedExecutionHandler 
{
	WorkerPool<T> pool;
	
	/**
	 * Object of message for which worker pool reject the worker
	 */
	Message message;
	
	/**
	 * Object of connection for which worker pool reject the worker
	 */
	Connection connection;
	
	@SuppressWarnings("unchecked")
	@Override
	public void rejectedExecution(Runnable worker, ThreadPoolExecutor workerPool) 
	{
		pool = (WorkerPool<T>)workerPool;
		message = ((Worker)worker).getMessage();
		connection = ((Worker)worker).getConnection();
	}

}
