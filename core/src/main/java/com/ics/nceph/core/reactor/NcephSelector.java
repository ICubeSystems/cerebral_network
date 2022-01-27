package com.ics.nceph.core.reactor;

import java.io.IOException;
import java.nio.channels.Selector;

public class NcephSelector 
{
	private Selector selector;
	
	public NcephSelector()
	{
		try 
		{
			selector = Selector.open();
		} catch (IOException e) 
		{
			//logger.error("Error in opening the selector");
			e.printStackTrace();
		} 
	}
	
	public Selector getSelector() {
		return selector;
	}
}
