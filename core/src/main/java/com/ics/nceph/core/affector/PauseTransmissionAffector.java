package com.ics.nceph.core.affector;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
 * This class executes within a write worker thread after the channel write operation is done (after sending PAUSE_TRANSMISSION message).<br>
 * @author Anshul
 * @since 22-Nov-2022
 */
public class PauseTransmissionAffector extends Affector 
{
	public PauseTransmissionAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		
	}
}
