package com.ics.synapse.affector;

import com.ics.nceph.core.affector.Affector;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.message.DocumentStore;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.message.ProofOfDelivery;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 16-Mar-2022
 */
public class PublishedEventAffector extends Affector 
{

	public PublishedEventAffector(Message message, Connection incomingConnection) 
	{
		super(message, incomingConnection);
	}

	@Override
	public void process() 
	{
		// Load the POD for this message
		ProofOfDelivery pod = DocumentStore.load(getMessage().decoder().getId());
		// Set the WriteRecord in the POD
		System.out.println("Write record::::::::" + getMessage().getWriteRecord().getStart() + " - " + getMessage().getWriteRecord().getEnd());
		pod.setWriteRecord(getMessage().getWriteRecord());
		// Save the POD
		DocumentStore.save(pod, getMessage().decoder().getId());
		
		System.out.println("POD [name: " + getMessage().decoder().getId() + ".json] updated with WriteRecord");
	}
}
