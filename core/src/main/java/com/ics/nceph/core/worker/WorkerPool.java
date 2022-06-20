package com.ics.nceph.core.worker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 * @param <T extends Worker>
 */
public class WorkerPool<T extends Worker> extends ThreadPoolExecutor
{
	private AtomicLong activeWorkers;
	
	private AtomicLong totalWorkersCreated;
	
	private AtomicLong totalSuccessfulWorkers;
	/**
     * Creates a new {@code WorkerPool} with the given initial parameters and the {@linkplain Executors#defaultThreadFactory default thread factory}.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are executed.  This queue will hold only the {@code Runnable} tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} or {@code handler} is null
     */
	public WorkerPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,	BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) 
	{
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, (BlockingQueue<Runnable>) workQueue, handler);
		this.activeWorkers = new AtomicLong(0);
		this.totalSuccessfulWorkers = new AtomicLong(0);
		this.totalWorkersCreated = new AtomicLong(0);
	}
	
	

	@Override
	protected void beforeExecute(Thread t, Runnable worker) 
	{
		// 1. Increment activeWorkers
		activeWorkers.incrementAndGet();
		// 2. Increment totalWorkersCreated
		totalWorkersCreated.incrementAndGet();
		
		super.beforeExecute(t, worker);
	}
	
	@Override
	protected void afterExecute(Runnable worker, Throwable t) 
	{
		// Call the super afterExecute
		super.afterExecute(worker, t);
		// 1. Decrement activeWorkers
		activeWorkers.decrementAndGet();
		// 2. Increment totalSuccessfulWorkers if the throwable is null
		if (t==null)
			totalSuccessfulWorkers.incrementAndGet();
	}
	
	public AtomicLong getActiveWorkers() {
		return activeWorkers;
	}

	public AtomicLong getTotalWorkersCreated() {
		return totalWorkersCreated;
	}

	public AtomicLong getTotalSuccessfulWorkers() {
		return totalSuccessfulWorkers;
	}
	
	/**
	 * 
	 * @author Anshul
	 * @since 01-Jun-2022
	 * @param <T>
	 */
	public static class Builder<T extends Worker>
	{
		Integer corePoolSize = 10;
		
		Integer maximumPoolSize = 100;
		
		Integer keepAliveTime = 60;
		
		TimeUnit unit = TimeUnit.SECONDS;
		
		BlockingQueue<Runnable> workQueue;
		
		RejectedExecutionHandler rejectedThreadHandler;
		
		/**
		 * The number of threads to keep in the pool, even if they are idle, unless allowCoreThreadTimeOut is set
		 * 
		 * @param corePoolSize
		 * @return Builder<T>
		 */
		public Builder<T> corePoolSize(Integer corePoolSize) {
			this.corePoolSize = corePoolSize;
			return this;
		}
		
		/**
		 * The maximum number of threads to allow in the pool
		 * 
		 * @param maximumPoolSize
		 * @return Builder<T>
		 */
		public Builder<T> maximumPoolSize(Integer maximumPoolSize) {
			this.maximumPoolSize = maximumPoolSize;
			return this;
		}
		
		/**
		 * Keep alive time in seconds - which is the amount of time that threads in excess of the core pool size may remain idle before being terminated.
		 * 
		 * @param keepAliveTime
		 * @return Builder<T>
		 */
		public Builder<T> keepAliveTime(Integer keepAliveTime) {
			this.keepAliveTime = keepAliveTime;
			return this;
		}
		
		/**
		 * The queue to use for holding tasks before they are executed. This queue will hold only the Runnable tasks submitted by the execute method.
		 * 
		 * @param workQueue
		 * @return Builder<T>
		 */
		public Builder<T> workQueue(BlockingQueue<Runnable> workQueue) {
			this.workQueue = workQueue;
			return this;
		}
		
		/**
		 * The handler to use when execution is blocked because the thread bounds and queue capacities are reached
		 * 
		 * @param handler
		 * @return Builder<T>
		 */
		public Builder<T> rejectedThreadHandler(RejectedExecutionHandler handler) {
			this.rejectedThreadHandler = handler;
			return this;
		}
		
		public WorkerPool<T> build()
		{
			return new WorkerPool<T>(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, rejectedThreadHandler);
		}
	}
}
