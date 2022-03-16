package com.ics.synapse.receptor;

import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.receptor.EventReceptor;

public class RelayedEventReceptor extends EventReceptor 
{
	public RelayedEventReceptor(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		System.out.println("In RelayedEventReceptor Process:::::::");
		System.out.println("Event ::::: " + getEvent().getEventId() + "-" + getEvent().getObjectJSON());
	}
}
