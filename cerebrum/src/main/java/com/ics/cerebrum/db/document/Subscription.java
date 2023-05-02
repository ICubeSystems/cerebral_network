package com.ics.cerebrum.db.document;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Anshul
 * @version V_1_0
 * @since 28-Jul-2022
 */
@Getter
@Setter
@DynamoDBDocument
public class Subscription 
{
	/**
	 * Type of event
	 */
	private Integer eventType;
	
	/**
	 * ApplicationReceptor class for this type of event
	 */
	private String applicationReceptor;
	
	/**
	 * Event class for this type of event
	 */
	private String eventClass;
}