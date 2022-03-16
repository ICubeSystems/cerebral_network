package com.ics.cerebrum.affector;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public class RelayedEventAffector extends Affector 
{

	public RelayedEventAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// TODO Auto-generated method stub
	}
}
