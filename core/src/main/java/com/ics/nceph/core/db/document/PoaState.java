package com.ics.nceph.core.db.document;

/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 16-May-2022
 */
public class PoaState 
{
	private int connectioState;

	PoaState() {}

	PoaState(int state) 
	{
		this.connectioState = state;
	}

	public int getState() 
	{
		return connectioState;
	}

	public static final PoaState INITIAL = new PoaState(100);
	public static final PoaState STARTUP = new PoaState(200);
	public static final PoaState AUTHENTICATE = new PoaState(300);
	public static final PoaState CREDENTIALS = new PoaState(400);
	public static final PoaState READY = new PoaState(500);
	public static final PoaState AUTH_ERROR = new PoaState(600);
	public static final PoaState READYCONFIRMED = new PoaState(700);
	
}
