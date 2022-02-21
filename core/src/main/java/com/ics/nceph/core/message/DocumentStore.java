package com.ics.nceph.core.message;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.Configuration;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 04-Feb-2022
 */
public class DocumentStore 
{
	private static final String LOCAL_MESSAGE_STORE_LOCATION = String.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.location"));
	
	private static final ObjectMapper mapper = new ObjectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"));
	
	/**
	 * Create a new ProofOfDelivery document or save the updates in the local document store
	 * 
	 * @param pod
	 * @param docName
	 * @return void
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 04-Feb-2022
	 */
	public static void save(ProofOfDelivery pod, String docName)
	{
	    try 
	    {
	    	mapper.writeValue(Paths.get(LOCAL_MESSAGE_STORE_LOCATION + docName + ".json").toFile(), pod);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads the ProofOfDelivery from the local document store
	 * 
	 * @param docName
	 * @return ProofOfDelivery
	 *
	 * @author Anurag Arya
	 * @version 1.0
	 * @since 04-Feb-2022
	 */
	public static ProofOfDelivery load(String docName)
	{
		try 
		{
			return mapper.readValue(Paths.get(LOCAL_MESSAGE_STORE_LOCATION + docName + ".json").toFile(), ProofOfDelivery.class);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
