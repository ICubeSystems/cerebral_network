package com.ics.nceph.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 25-Jan-2022
 */
public class Configuration 
{
	Properties propertyFile;
	
	private Configuration(String fileName)
	{
		this.propertyFile = new Properties();
		InputStream inputStream = Configuration.class.getClassLoader().getResourceAsStream(fileName);
		try {
			propertyFile.load(inputStream);
			inputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getConfig(String configName) 
	{
	    String property = propertyFile.getProperty(configName);
		return property;
	}
	
	public Integer getConfigAsInteger(String configName) 
	{
	    String property = propertyFile.getProperty(configName);
		return Integer.valueOf(property);
	}
	
	public static Configuration APPLICATION_PROPERTIES = new Configuration("application.properties");
}
