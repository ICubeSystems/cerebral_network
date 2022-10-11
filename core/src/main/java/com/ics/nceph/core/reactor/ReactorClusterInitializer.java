package com.ics.nceph.core.reactor;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;

import com.ics.util.OSInfo;

/**
 * Factory class to create {@link ReactorCluster} depending on the number of cores in the system
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 22-Dec-2021
 */
public class ReactorClusterInitializer 
{
	@Autowired
	private OSInfo osInfo;
	
	public ReactorCluster initializeReactorCluster() throws IOException 
	{
		// 1. Initialize the reactorCluster
		ReactorCluster reactorCluster = new ReactorCluster();
		
		if(osInfo == null)
			osInfo = new OSInfo();
		
		// 2. Get the number of CPU cores available
		int coresCount = osInfo.numberOfCPUCores*2;
		System.out.println("Number of CPU cores: "+coresCount);
		
		// 3. Create a Reactor instance per CPU core and add them to the reactor cluster
		for (Integer i = 1; i <= coresCount; i++)
		{
			System.out.println("Creating Reactor " + i);
			Reactor reactor = new Reactor(i);
			reactorCluster.add(reactor);
		}
		
		return reactorCluster;
	}
}
