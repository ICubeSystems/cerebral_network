package com.ics.nceph.core.document;

/**
 * 
 * @author Anshul
 * @since 25-Apr-2022
 */
public class PorState {
	private int state;
	PorState(){
		
	}
	PorState(int state)
	{
		this.state = state;
	}
	
	public int getState() {
		return state;
	}
	/**
	 * this is the INITIAL state of POR. when por is created its state is set to initial.
	 */
	public static final PorState INITIAL = new PorState(100);
	/**
	 * this is the RELAYED state of POR. when event is relayed to subscriber state of por is set to RELAYED.
	 */
	public static final PorState RELAYED = new PorState(200);
	/**
	 * this is the ACKNOWLEDGED state of POR. When RELAY_EVENT is acknowledged then state of por is set to ACKNOWLEDGED.
	 */
	public static final PorState ACKNOWLEDGED = new PorState(300);
	/**
	 * this is the ACK_RECIEVED state of POR. When RELAY_EVENT is threeWayAcknowledged then state of por is set to ACK_RECIEVED.
	 */
	public static final PorState ACK_RECIEVED = new PorState(400);
	/**
	 * this is the ACK_RECIEVED state of POR. When RELAY_EVENT is fully acknowledged then state of por is set to FINISHED.
	 */
	public static final PorState FINISHED = new PorState(500);
	
}
