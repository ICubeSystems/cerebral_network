package com.ics.nceph.core.affector;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;

/**
 * This class executes within a write worker thread after the channel write operation is done (after sending RESUME_TRANSMISSION message).<br>
 * @author Anshul
 * @version 1.0
 * @since Nov 22, 2022
 */
public class ResumeTransmissionAffector extends Affector 
{
	public ResumeTransmissionAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		
	}
}
