package com.ics.cerebrum.mac;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.ics.logger.BootstraperLog;
import com.ics.logger.NcephLogger;
import com.ics.nceph.config.exception.NodeResolutionException;

/**
 * 
 * @author Anshul
 * @version 1.0
 * @since 28-Jul-2022
 */
public class SynapticMappingStore
{
	private static SynapticMACMapping synapticMappingCache;
	
	private static final ObjectMapper mapper = new ObjectMapper()
			.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
			.enable(SerializationFeature.INDENT_OUTPUT)
			.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
			.setSerializationInclusion(Include.NON_NULL);

	/**
	 * This method is used to initialize and build the node name resolver cache. This method is called via the cerebral bootstrapping.
	 */
	public static void initiate() 
	{
		synapticMappingCache = new SynapticMACMapping();
		File file = new File(synapticMappingCache.synapticMappingLocation()+synapticMappingCache.synapticMappingFileName());
		if(file.exists()) 
		{
			try 
			{
				synapticMappingCache = mapper.readValue(Paths.get(synapticMappingCache.synapticMappingLocation()+synapticMappingCache.synapticMappingFileName()).toFile(),SynapticMACMapping.class);
				return;
			} catch (IOException e) 
			{
				NcephLogger.BOOTSTRAP_LOGGER.error(new BootstraperLog.Builder()
						.action("Error building idCache")
						.description(e.getLocalizedMessage())
						.logError(),e);
			}
		}
	}

	/**
	 * This method looks for a nodeId with the given MAC address. 
	 * If the MAC address is not in the registry then it creates a new entry and returns the newly created nodeId.
	 * 
	 * @return NodeId - unique identifier of a synaptic node in the nceph network
	 * @throws NodeResolutionException 
	 */
	public static Integer resolveNode(String macId) throws NodeResolutionException
	{
		// TODO: In future add a configuration for manual vs automatic registry of synaptic nodes
		// Check if MAC address is not already registered
		if(synapticMappingCache.getMacMapping().get(macId) == null) 
		{
			// Create new nodeId and add to the map
			synapticMappingCache.getMacMapping().put(macId, synapticMappingCache.getLastUsedId().incrementAndGet());
			// Save the changes to the file
			save();
		}
		// return the nodeId
		return synapticMappingCache.getMacMapping().get(macId);
	}

	/**
	 * Create a new SynapticMapping.json or save the updates in the local store
	 * 
	 * @throws NodeResolutionException
	 * @version 1.0
	 * @since Aug 2, 2022
	 */
	public synchronized static void save() throws NodeResolutionException
	{
		try 
		{
			// Save the SynapticMapping to the local storage
			mapper.writeValue(Paths.get(synapticMappingCache.synapticMappingLocation() + synapticMappingCache.synapticMappingFileName()).toFile(), synapticMappingCache);
		} 
		catch (IOException e) // In case the message directory is missing 
		{
			// If SynapticMapping.json/ directory is not found at the expected location, then create the missing directory & file.
			if (e instanceof FileNotFoundException)
			{
				// Create new directory
				new File(Paths.get(synapticMappingCache.synapticMappingLocation()).toString()).mkdirs();
				// Create & save SynapticMapping.json
				try 
				{
					mapper.writeValue(Paths.get(synapticMappingCache.synapticMappingLocation() + synapticMappingCache.synapticMappingFileName()).toFile(), synapticMappingCache);
					return;
				} catch (IOException e1){}
			}
			// Log and throw exception in case exception is not FileNotFoundException 
			NcephLogger.GENERAL_LOGGER.error(new BootstraperLog.Builder()
					.action("SynapticMapping sync failed")
					.description(e.getLocalizedMessage())
					.logError(),e);
			throw new NodeResolutionException("SynapticMapping sync failed", e);
		}
	}
}
