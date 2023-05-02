package com.ics.nceph.core.db.document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * @author Anshul
 * @version 1.0.2
 * @since May 1, 2023
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationReceptorMetaData
{
	/**
	 * ApplicationReceptor class for this type of event
	 */
	private String applicationReceptorFQCN;
	
	/**
	 * Event class for this type of event
	 */
	private String eventObjectFQCN;
}
