package com.ics.cerebrum.configuration;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.ics.cerebrum.configuration.exception.ConfigurationException;
import com.ics.cerebrum.db.document.SynapticNodesList;

/**
 * Interface for Cerebral Configuration for a specific type.
 * @author Anshul
 * @version 1.0
 * @since Sep 19, 2022
 */
public interface CerebralConfiguration 
{
	final ObjectMapper mapper = new ObjectMapper()
			.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
			.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.setSerializationInclusion(Include.NON_NULL);

	public Integer getNodeIdForKey(String secretKey) throws ConfigurationException;

	public SynapticNodesList getSynapticNodes() throws ConfigurationException;
}
