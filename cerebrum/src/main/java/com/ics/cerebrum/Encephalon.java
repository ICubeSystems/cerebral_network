package com.ics.cerebrum;

import java.io.File;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import com.ics.nceph.core.message.MessageLedger;

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
	    	System.out.println("2.) Connector Status");
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
	    		if(messageDirectory.listFiles() == null)
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
    				System.out.println("No connector available on port no "+choice);
    				System.out.println("Enter port number from list");
    				choice = input.nextInt();
    			}
    			if(choice == 404)
    				break connectorLoop;
    			
    			CerebralConnector connector = (CerebralConnector)ConnectorCluster.activeConnectors.get(choice);
    			
    			System.out.println("\n Connector Status 1");
    			System.out.printf("| %6s | %20s | %24s | %14s | %21s | %24s | %16s | \n",
    					"PORT",
    					"Active_Connections",
    					"Total_Connections_Served",
    					"Active_Workers",
    					"Total_Workers_Created",
    					"Total_Successful_Workers",
    					"Relay_Queue_Size");
    			System.out.printf("| %6d | %20d | %24d | %14d | %21s | %24s | %16s | \n",
    					connector.getPort(),
    					connector.getActiveConnections().size(),
    					connector.getTotalConnectionsServed(),
    					connector.getReaderPool().getActiveWorkers().intValue()+ connector.getWriterPool().getActiveWorkers().intValue(),
    					connector.getReaderPool().getTotalWorkersCreated().intValue()+ connector.getWriterPool().getTotalWorkersCreated().intValue(),
    					connector.getReaderPool().getTotalSuccessfulWorkers().intValue()+ connector.getWriterPool().getTotalSuccessfulWorkers().intValue(),
    					connector.getRelayQueue().size()
    					);
    			System.out.println("\n Connector Status 2");
    			System.out.printf("| %6s | %20s | %20s | %30s | %30s | \n",
    					"PORT",
    					"Incoming_Register",
    					"Outgoing_Register",
    					"Connector_Queued_Up_Register",
    					"Connection_Queued_Up_Register"
    					);
    			
    			System.out.printf("| %6d | %20d | %20d | %30d | %30s | \n",
    					connector.getPort(),
    					connector.getIncomingMessageRegister().size(),
    					connector.getOutgoingMessageRegister().size(),
    					connector.getConnectorQueuedUpMessageRegister().size(),
    					connector.getConnectionQueuedUpMessageRegister().size()
    					);
    			System.out.println("\nConnections Status");
    			System.out.printf("| %13s | %18s | %16s | %21s | %31s | %15s | %10s | \n",
    					"Connection_id",
    					"State",
    					"Active_Requests",
    					"Total_Requests_Served",
    					"Total_Successful_Requests_Served",
    					"Relay_QueueSize",
    					"Idle_Since");
    			
    			for (ConcurrentHashMap.Entry<Integer, Connection> entry : connector.getActiveConnections().entrySet()) {
    				Connection connection = entry.getValue();
    				System.out.printf("| %13d | %18s | %16d | %21d | %32s | %15s | %10s | \n",
    						connection.getId(),
    						connection.getState().getState(),
    						connection.getActiveRequests().intValue(),
    						connection.getTotalRequestsServed().intValue(),
    						connection.getTotalSuccessfulRequestsServed().intValue(),
    						connection.getRelayQueue().size(),
    						connection.getIdleTime()
        					);
				}
    			System.out.println("\nRelayed messages");
    			System.out.printf("| %8s | %10s | %15s | \n",
    					"SourceId",
    					"Event_Type",
    					"Total_Published");
    			for (ConcurrentHashMap.Entry<Integer, MessageLedger> entry : connector.getOutgoingMessageRegister().getMasterLedger().entrySet()) {
    				MessageLedger messageLedger = entry.getValue();
    				for (ConcurrentHashMap.Entry<Integer, Set<Long>> entry1 : messageLedger.getLedger().entrySet()) {
        				
    					System.out.printf("| %8d | %10d | %15d | \n",
    							entry.getKey(),
    	    					entry1.getKey(),
    	    					entry1.getValue().size());
    					
    				}
				}
    			System.out.println("\nRecieved messages");
    			System.out.printf("| %8s | %10s | %15s | \n",
    					"SourceId",
    					"Event_Type",
    					"Total_Recieved");
    			for (ConcurrentHashMap.Entry<Integer, MessageLedger> entry : connector.getIncomingMessageRegister().getMasterLedger().entrySet()) {
    				MessageLedger messageLedger = entry.getValue();
    				for (ConcurrentHashMap.Entry<Integer, Set<Long>> entry1 : messageLedger.getLedger().entrySet()) {
        				
    					System.out.printf("| %8s | %10d | %15d | \n",
    							entry.getKey(),
    	    					entry1.getKey(),
    	    					entry1.getValue().size());
    					
    				}
				}
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
