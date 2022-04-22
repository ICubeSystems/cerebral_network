package com.ics.cerebrum.affector;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 30-Mar-2022
 */
public class DeletePodAffector extends Affector 
{

	public DeletePodAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		
	}
}
