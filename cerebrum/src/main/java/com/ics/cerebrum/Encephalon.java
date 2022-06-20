package com.ics.cerebrum;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import com.ics.cerebrum.bootstrap.Bootstraper;
import com.ics.cerebrum.connector.CerebralConnector;
import com.ics.console.Console;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;

@SpringBootApplication
@ImportResource("classpath:nceph_context.xml")
public class Encephalon implements CommandLineRunner
{
	@Autowired
	private Bootstraper bootstraper;
	
	public static void main(String[] args) 
	{
		SpringApplication nceph = new SpringApplication(Encephalon.class);
		nceph.setBannerMode(Banner.Mode.OFF);
		// @todo - Create Banner for the project and set it here for logging - use the Banner class provided by Spring.
		nceph.run(args);
	}
	
	@Override
	public void run(String... args) throws Exception 
	{
		// 1. Initialize the bootstraper
		bootstraper.boot();
		
		// 2. Render the menu to monitor the running of Encephelon
		Scanner input = new Scanner(System.in);
	    int choice;
	    menuLoop: while(true)
	    {
	    	System.out.println("Menu");
	    	System.out.println("1.) Cerebral status");
	    	System.out.println("2.) Connector");
	    	System.out.println("3.) TLS_MODE");
	    	System.out.println("4.) Shutdown.");
	    	System.out.println("\nEnter Your Menu Choice: ");

	    	choice = input.nextInt();
	    	switch(choice)
	    	{
	    	case 1:
	    		// Get the messages directory
	    		File messageDirectory = new File(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location"));
	    		// If there are no pods the print the no pod found message
	    		if(messageDirectory.listFiles().length == 0)
	    		{
	    			Console.error("POD's not found in Cerebrum");
	    			break;
	    		}
	    		
	    		// Loop through the messages directory and check for the validity of the PODs
	    		float totalPods = messageDirectory.listFiles().length;
	    		int invalidPods = 0;
	    		float loopCounter = 0;
	    		ArrayList<String> invalidPodsArray = new ArrayList<>();
	    		System.out.print("Loading...");
	    		for (File podFile : messageDirectory.listFiles()) 
	    		{
	    			ProofOfDelivery pod = DocumentStore.load(podFile);
	    			// Print the validity status of the POD
//	    			String status = pod.validate();
	    			
	    			if(pod.validate()!="") {
//	    				Console.error(podFile.getName()+" ✕ "+status);
	    				invalidPods++;
	    				invalidPodsArray.add(podFile.getName());
	    			}
//	    			else
//	    				Console.success(podFile.getName()+" ✓ ");
	    			loopCounter++;
	    			float percentage = (loopCounter/totalPods)*100;
	    			System.out.print((int)percentage+"%");
	    			if(percentage<10)
	    			System.out.print("\b\b");
	    			else if(percentage<100)
	    			System.out.print("\b\b\b");
	    			else
	    			System.out.println("\n");
	    		}
	    		Console.success("Total pods : " + (int)totalPods);
	    		Console.error("Invalid pods: "+invalidPods);
	    		Console.error("Invalid pods list: "+ invalidPodsArray.toString());
	    		break;
	    	case 2:
	    		connectorLoop: while(true) {
	    		System.out.println("Connectors");
    			int count = 1;
    			for (Entry<Integer, Connector> entry : ConnectorCluster.activeConnectors.entrySet())
    			{
    				CerebralConnector connector = (CerebralConnector)entry.getValue();
    				System.out.println(count+".) connector on port no "+connector.getPort());
    				count++;
    			}
    			System.out.println("Enter port number from list to check detail of connector or 404 for exit");
    			choice = input.nextInt();
    			while(ConnectorCluster.activeConnectors.get(choice)==null && choice!=404) {
    				System.out.println("no connector available on port no "+choice);
    				System.out.println("Enter port number from list");
    				choice = input.nextInt();
    			}
    			if(choice == 404)
    				break connectorLoop;
    			
    			CerebralConnector connector = (CerebralConnector)ConnectorCluster.activeConnectors.get(choice);
    			System.out.println("Connector relay size: "+connector.getRelayQueue().size());
    			System.out.println("Connector active read workers: " + connector.getReaderPool().getActiveCount());
    			System.out.println("Connector total read workers: " + connector.getReaderPool().getTotalWorkersCreated());
    			System.out.println("Connector successful read workers: " + connector.getReaderPool().getTotalSuccessfulWorkers());
    			System.out.println("Connector active write workers: " + connector.getWriterPool().getActiveCount());
    			System.out.println("Connector total write workers: " + connector.getWriterPool().getTotalWorkersCreated());
    			System.out.println("Connector successful write workers: " + connector.getWriterPool().getTotalSuccessfulWorkers());
    			System.out.println("Active connections: "+connector.getActiveConnections().size());
    			count = 1;
    			for (Entry<Integer, Connection> entry : connector.getActiveConnections().entrySet()) {
    				Connection connection = entry.getValue();
    				System.out.println(count+".) Connection Id: "+connection.getId());
    				System.out.println("Connection's relay queue size: "+connection.getRelayQueue().size());
    				System.out.println("Served "+ connection.getTotalRequestsServed());
    				System.out.println();
    				count++;
				}
    			connector.getActiveConnections();
	    		}
	    		break;
	    	case 3:
	    		System.out.println("TLS_MODE is set to "+NcephConstants.TLS_MODE);
	    		break;
	    	case 4:
	    		//TODO: handle the proper shutdown of the cerebrum
	    		System.out.println("COMING SOON....");
	    		input.close();
	    		break menuLoop;
	    		
	    	default:
	    		System.out.println("Invalid selection....");
	    	}
	    }

	}
}
