package com.ics.nceph.core.message;

import java.util.Date;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 08-Mar-2022
 */
public class ProofOfRelay 
{
	private Date relayedOn;
	
	private Date deliveredOn;
	
	private Date ackReceivedOn;
	
	private int destinationPort;
}
