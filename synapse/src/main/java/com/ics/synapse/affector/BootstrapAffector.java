package com.ics.synapse.affector;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Aug 2, 2022
 */
public class BootstrapAffector extends Affector
{
	public BootstrapAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
	}
}
