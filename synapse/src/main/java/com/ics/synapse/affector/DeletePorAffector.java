package com.ics.synapse.affector;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
 * 
 * @author Anshul
 * @version 1.0
 * * @since 10-Apr-2022
 */
public class DeletePorAffector extends Affector 
{

	public DeletePorAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
	}
}
