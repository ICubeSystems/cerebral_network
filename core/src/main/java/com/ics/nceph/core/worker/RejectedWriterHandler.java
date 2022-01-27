package com.ics.nceph.core.worker;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * A handler for {@link Writer} task that cannot be executed by a {@link WorkerPool}.
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 23-Dec-2021
 */
public class RejectedWriterHandler implements RejectedExecutionHandler 
{
	@Override
	/**
     * Method that may be invoked by a {@link ThreadPoolExecutor} when {@link ThreadPoolExecutor#execute execute} cannot accept a task.  
     * This may occur when no more threads or queue slots are available because their bounds would be exceeded, or upon shutdown of the Executor.
     *
     * <p>In the absence of other alternatives, the method may throw an unchecked {@link RejectedExecutionException}, which will be propagated to the caller of {@code execute}.
     *
     * @param worker - the runnable task requested to be executed
     * @param workerPool - the executor attempting to execute this task
     * @throws RejectedExecutionException if there is no remedy
     */
	public void rejectedExecution(Runnable worker, ThreadPoolExecutor workerPool) 
	{
		// TODO Handle the rejection here - may be we can re try to execute the thread in any other connection or wait for some time and then try to ecexute again in the same workerPool. 
		// Leaving this implementation for later.
	}
}
