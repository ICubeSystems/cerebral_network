package com.ics.receptor;

import com.ics.nceph.core.event.EventData;
import com.ics.synapse.applicationReceptor.ApplicationReceptor;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since Aug 1, 2022
 */
public class GiftStatusReceptor extends ApplicationReceptor {

	public GiftStatusReceptor(EventData eventData) {
		super(eventData);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub
		System.out.println("Gift item receptor");
	}

	

}
