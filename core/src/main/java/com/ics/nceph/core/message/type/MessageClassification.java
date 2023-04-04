package com.ics.nceph.core.message.type;

/**
 * MessageClassification ENUM
 * @author Anshul
 * @version V_6_0 
 * @since Apr 3, 2023
 */
public enum MessageClassification
{
	PUBLISH(100),
	RELAY(200),
	AUTHENICATION(300),
	BACKPRESSURE(400),
	CONTROL(500);

	int classification;
	
	MessageClassification(int i) {
		this.classification = i;
	}
}
