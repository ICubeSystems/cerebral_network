package com.ics.env;

import com.ics.nceph.core.Configuration;

/**
 * 
 * @author Anshul
 * @since 28-May-2022
 */
public class Environment 
{
	private int environment;
	
	Environment(int environment)
	{
		this.environment = environment;
	}
	
	public int getValue() {
		return environment;
	}
	
	public static final Environment DEVELOPER = new Environment(100);
	
	public static final Environment UAT = new Environment(200);
	
	public static final Environment PRODUCTION = new Environment(300);
	
	public static boolean isDev() {
		return Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("application.environment")) == Environment.DEVELOPER.getValue();
	}
	
	public static boolean isProd() {
		return Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("application.environment")) == Environment.PRODUCTION.getValue();
	}
	
	public static boolean isUat() {
		return Integer.valueOf(Configuration.APPLICATION_PROPERTIES.getConfig("application.environment")) == Environment.UAT.getValue();
	}
}
