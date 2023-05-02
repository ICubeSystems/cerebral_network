package com.ics.menu;

import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ics.menu.eventThreads.EventThread;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.message.MessageLedger;
import com.ics.synapse.connector.SynapticConnector;

/**
 * Terminal menu for synaptic application
 * @author Anshul
 * @version 1.0
 * @since Sep 30, 2022
 */

public class SynapticMenu
{
	public static void run(SynapticConnector connector) {
		// 1. Render the menu to monitor the running of Synapse
				Scanner input = new Scanner(System.in);
				int choice;
				menuLoop: while(true)
			    {
					System.out.println("Menu");
			    	System.out.println("1.) Publish_Event");
			    	System.out.println("2.) Connector Status");
			    	System.out.println("3.) Shutdown.");
			    	System.out.println("\nEnter Your Menu Choice: ");
			    	
			    	choice = input.nextInt();
			    	
			    	switch (choice) {
					case 1:
						int eventChoice, numberOfEvents;
						eventLoop: while(true) {
						System.out.println("Choose event from list: ");
						int count = 1;
						for (EventType giftType : EventType.types) {
							System.out.println(count + ".) "+giftType.getGiftClass().getSimpleName());
							count++;
						}
						System.out.println(count + ".) Back");
						eventChoice = input.nextInt();
						try {
							System.out.println("Selected event is "+EventType.getEventType(eventChoice).getGiftClass().getSimpleName());
							System.out.print("How many messages do you want to publish: ");
							numberOfEvents = input.nextInt();
							EventThread thread = new EventThread.Builder()
									.numberOfEvents(Integer.valueOf(numberOfEvents))
									.implementationClass(EventType.getEventType(eventChoice).getCallingclass())
									.build();
							thread.run();
								
						} catch (Exception e) {
							// TODO Auto-generated catch block
							if(eventChoice == count)
								break eventLoop;
							System.out.println("Wrong entry please retry");
						}
						}
						break;
					case 2:
						// 3. Connector status
						// 3.1 this table shows connector's Active connections, total connections served, Active_Workers, Total workers created, Total successful workers, Relay queue size
						System.out.println("\nConnector Status 1");
						System.out.println(" _________________________________________________________________________________________________________________________________________________");
		    			System.out.printf("| %6s | %20s | %24s | %14s | %21s | %24s | %16s | \n",
		    					"PORT",
		    					"Active_Connections",
		    					"Total_Connections_Served",
		    					"Active_Workers",
		    					"Total_Workers_Created",
		    					"Total_Successful_Workers",
		    					"Relay_Queue_Size");
		    			System.out.println(" -------------------------------------------------------------------------------------------------------------------------------------------------");
		    			System.out.printf("| %6d | %20d | %24d | %14d | %21s | %24s | %16s | \n",
		    					connector.getPort(),
		    					connector.getActiveConnections().size(),
		    					connector.getTotalConnectionsServed(),
		    					connector.getRelayReaderPool().getActiveWorkers().intValue()+ connector.getRelayWriterPool().getActiveWorkers().intValue()
								+ connector.getPublishReaderPool().getActiveWorkers().intValue()+ connector.getPublishWriterPool().getActiveWorkers().intValue(),
								connector.getRelayReaderPool().getTotalWorkersCreated().intValue()+ connector.getRelayWriterPool().getTotalWorkersCreated().intValue()
								+ connector.getPublishReaderPool().getTotalWorkersCreated().intValue()+ connector.getPublishWriterPool().getTotalWorkersCreated().intValue(),
								connector.getRelayReaderPool().getTotalSuccessfulWorkers().intValue()+ connector.getRelayWriterPool().getTotalSuccessfulWorkers().intValue()
								+ connector.getPublishReaderPool().getTotalSuccessfulWorkers().intValue()+ connector.getPublishWriterPool().getTotalSuccessfulWorkers().intValue(),
		    					connector.getRelayQueue().size()
		    					);
		    			System.out.println(" _________________________________________________________________________________________________________________________________________________");
		    			// 3.2 This table shows connector's registers status
		    			System.out.println("\nConnector Status 2");
		    			System.out.println(" ________________________________________________________________________________________________________________________");
		    			System.out.printf("| %6s | %20s | %20s | %30s | %30s | %20s | \n",
		    					"PORT",
		    					"Incoming_Register",
		    					"Outgoing_Register",
		    					"Connector_Queued_Up_Register",
		    					"Connection_Queued_Up_Register",
		    					"Publish Cache size"
		    					);
		    			System.out.println(" ------------------------------------------------------------------------------------------------------------------------");
		    			System.out.printf("| %6d | %20d | %20d | %30d | %30s | %20s | \n",
		    					connector.getPort(),
		    					connector.getIncomingMessageRegister().size(),
		    					connector.getOutgoingMessageRegister().size(),
		    					connector.getConnectorQueuedUpMessageRegister().size(),
		    					connector.getConnectionQueuedUpMessageRegister().size(),
		    					ProofOfPublish.getMessageCache(connector.getPort()) != null ? ProofOfPublish.getMessageCache(connector.getPort()).size() : 0
		    					);
		    			System.out.println(" ________________________________________________________________________________________________________________________");
		    			// 3.3 This table shows connector's connections status
		    			System.out.println("\nConnections Status");
		    			System.out.println(" _________________________________________________________________________________________________________________________________________________");
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
		    				System.out.println(" -------------------------------------------------------------------------------------------------------------------------------------------------");
		    				System.out.printf("| %13d | %18s | %16d | %21d | %32s | %15s | %10s | \n",
		    						connection.getId(),
		    						connection.getState().getState(),
		    						connection.getMetric().getActiveRequests().intValue(),
		    						connection.getMetric().getTotalRequestsServed().intValue(),
		    						connection.getMetric().getTotalSuccessfulRequestsServed().intValue(),
		    						connection.getRelayQueue().size(),
		    						connection.getIdleTime()
		        					);
		    				
						}
		    			System.out.println(" _________________________________________________________________________________________________________________________________________________");
		    			// 3.4 This table shows published messages
		    			System.out.println("\nPublished messages");
		    			System.out.println(" __________________________________________");
		    			System.out.printf("| %8s | %10s | %15s | \n",
		    					"SourceId",
		    					"Event_Type",
		    					"Total_Published");
		    			
		    			for (ConcurrentHashMap.Entry<Integer, MessageLedger> entry : connector.getOutgoingMessageRegister().getMasterLedger().entrySet()) {
		    				MessageLedger messageLedger = entry.getValue();
		    				for (ConcurrentHashMap.Entry<Integer, Set<Long>> entry1 : messageLedger.getLedger().entrySet()) {
		    					System.out.println(" ------------------------------------------");
		    					System.out.printf("| %8d | %10d | %15d | \n",
		    							entry.getKey(),
		    	    					entry1.getKey(),
		    	    					entry1.getValue().size());
		    					
		    				}
						}
		    			System.out.println(" __________________________________________");
		    			// 3.5 This table shows recieved messages
		    			System.out.println("\nRecieved messages");
		    			System.out.println(" __________________________________________");
		    			System.out.printf("| %8s | %10s | %15s | \n",
		    					"SourceId",
		    					"Event_Type",
		    					"Total_Recieved");
		    			for (ConcurrentHashMap.Entry<Integer, MessageLedger> entry : connector.getIncomingMessageRegister().getMasterLedger().entrySet()) {
		    				MessageLedger messageLedger = entry.getValue();
		    				for (ConcurrentHashMap.Entry<Integer, Set<Long>> entry1 : messageLedger.getLedger().entrySet()) {
		    					System.out.println(" ------------------------------------------");
		    					System.out.printf("| %8s | %10d | %15d | \n",
		    							entry.getKey(),
		    	    					entry1.getKey(),
		    	    					entry1.getValue().size());
		    					
		    				}
						}
		    			System.out.println(" __________________________________________");
		    			break;
					case 3:
						//TODO: handle the proper shutdown of the cerebrum
			    		System.out.println("COMING SOON....");
			    		input.close();
			    		break menuLoop;
					default:
						break;
					}
			    	
			    }
	}
}
