package com.ics.nceph.core.connector.connection;

/**
 * 
 * @author Anshul
 * @since 17-Jun-2022
 */
public class QueuingContext 
{
	private int context;
	
	private boolean checkDuplicacy;
	
	String logAction;
	
	QueuingContext(int context, boolean checkDuplicacy, String logAction)
	{
		this.context = context;
		this.checkDuplicacy = checkDuplicacy;
		this.logAction = logAction;
	}
	
	public int getContext() {
		return context;
	}

	public boolean duplicacyCheckEnabled() {
		return checkDuplicacy;
	}

	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final QueuingContext QUEUED_FROM_CONNECTOR = new QueuingContext(100, true, "Enqueued via connector");
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final QueuingContext QUEUED_FROM_EMITTER = new QueuingContext(200, true, "Enqueued via emitter");
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final QueuingContext QUEUED_FROM_RECEPTOR = new QueuingContext(300, true, "Enqueued via receptor");
	/**
	 * used to check the context from where messages was queued to connector or connection
	 */
	public static final QueuingContext QUEUED_FROM_MONITOR = new QueuingContext(400, false, "Enqueued via monitor");
}
