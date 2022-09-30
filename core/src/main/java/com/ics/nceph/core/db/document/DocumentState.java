package com.ics.nceph.core.db.document;

/**
 * 
 * @author Anshul
 * @since 25-Apr-2022
 */
public class DocumentState {
	private int state;
	DocumentState(){
		
	}
	DocumentState(int state)
	{
		this.state = state;
	}
	
	public int getState() {
		return state;
	}
	
	public static final DocumentState INITIAL = new DocumentState(100);
	public static final DocumentState PUBLISHED = new DocumentState(200);
	public static final DocumentState RELAYED = new DocumentState(300);
	public static final DocumentState ACKNOWLEDGED = new DocumentState(400);
	public static final DocumentState ACK_RECIEVED = new DocumentState(500);
	public static final DocumentState FINISHED = new DocumentState(600);
	
}
