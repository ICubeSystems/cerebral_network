package com.ics.nceph.core.document;

/**
 * 
 * @author Anshul
 * @since 25-Apr-2022
 */
public class PodState 
{
	private int state;
	
	PodState(){}
	
	PodState(int state)
	{
		this.state = state;
	}
	
	public int getState() {
		return state;
	}
	
	/**
	 * <b>On Synapse:</b> When pod is created its state is set to INITIAL<br>
	 * <b>On Cerebrum:</b> PODs on cerebrum does not have INITIAL state
	 */
	public static final PodState INITIAL = new PodState(100);
	
	/**
	 * <b>On Synapse:</b> When the message is completely written to the socket channel, then POD is set to Published<br>
	 * <b>On Cerebrum:</b> When the message is completely read from the socket channel, then POD is created & set to Published
	 */
	public static final PodState PUBLISHED = new PodState(200);
	
	/**
	 * <b>On Synapse:</b> When the NCEPH_EVENT_ACK message is completely read from the socket channel, then it is set to ACKNOWLEDGED<br>
	 * <b>On Cerebrum:</b> When the NCEPH_EVENT_ACK message is completely written on the socket channel, then the POD is set to ACKNOWLEDGED
	 */
	public static final PodState ACKNOWLEDGED = new PodState(300);
	
	/**
	 * <b>On Synapse:</b> When the ACK_RECEIVED message is completely written on the socket channel, then it is set to ACK_RECIEVED<br>
	 * <b>On Cerebrum:</b> When the ACK_RECEIVED message is completely read from the socket channel, then the POD is set to ACK_RECIEVED
	 */
	public static final PodState ACK_RECIEVED = new PodState(400);
	
	/**
	 * <b>On Synapse:</b> When the DELETE_POD message is completely read from the socket channel, then it is set to FINISHED<br>
	 * <b>On Cerebrum:</b> When the DELETE_POD message is completely written in the socket channel, then the POD is set to FINISHED
	 */
	public static final PodState FINISHED = new PodState(500);
	
	@Override
	public boolean equals(Object o)
	{
		PodState podState = (PodState) o;
		return podState.state == this.state ? true : false;
	}
}
