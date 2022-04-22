package com.ics.logger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Anshul
 * @since 06-Apr-2022
 */
public class NcephLogger 
{
	public static final Logger BOOTSTRAP_LOGGER = LogManager.getLogger("bootstraper");
	
	public static final Logger MESSAGE_LOGGER = LogManager.getLogger("message");
	
	public static final Logger CONNECTION_LOGGER = LogManager.getLogger("connection");
	
	public static final Logger GENERAL_LOGGER = LogManager.getLogger("general");
}
