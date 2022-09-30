package com.ics.cerebrum.db.document;

import com.ics.nceph.core.db.document.Document;

import lombok.Getter;
import lombok.Setter;

/**
 * Model class for node configurations
 * 
 * @author Anshul
 * @version 1.0
 * @since Sep 10, 2022
 */
@Getter
@Setter
public class NodeConfiguration extends Document
{
	/**
	 * Port number assigned to an application in the cerebrum.
	 */
	private Integer nodeId;
	
	/**
	 * Unique key to fetch node id from cerebrum during control connection.
	 */
	private String secretKey;
	
	/**
	 * Status of nodeId.
	 */
	private boolean isActive;
	
	/**
	 * Application port for this node.
	 */
	private Integer port;
}