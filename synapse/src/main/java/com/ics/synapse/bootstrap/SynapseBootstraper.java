package com.ics.synapse.bootstrap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.annotation.Value;

import com.ics.console.Console;
import com.ics.id.IdGenerator;
import com.ics.logger.NcephLogger;
import com.ics.menu.eventThreads.EventThread;
import com.ics.nceph.core.Configuration;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.connector.connection.exception.ConnectionException;
import com.ics.nceph.core.connector.exception.ImproperConnectorInstantiationException;
import com.ics.nceph.core.document.DocumentStore;
import com.ics.nceph.core.document.ProofOfDelivery;
import com.ics.nceph.core.message.MessageLedger;
import com.ics.nceph.core.reactor.ReactorCluster;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.nceph.core.ssl.NcephSSLContext;
import com.ics.nceph.core.ssl.exception.SSLContextInitializationException;
import com.ics.nceph.core.worker.Reader;
import com.ics.nceph.core.worker.RejectedReaderHandler;
import com.ics.nceph.core.worker.RejectedWriterHandler;
import com.ics.nceph.core.worker.WorkerPool;
import com.ics.nceph.core.worker.Writer;
import com.ics.synapse.Emitter;
import com.ics.synapse.connector.SynapticConnector;
import com.ics.synapse.message.type.SynapticIncomingMessageType;
import com.ics.synapse.message.type.SynapticOutgoingMessageType;
import com.ics.synapse.ncephEvent.EventType;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 13-Jan-2022
 */
public class SynapseBootstraper 
{
	ReactorCluster reactorCluster;
	
	@Value("${synapticConnector.name}")
	private String connectorName;
			
	@Value("${cerebrum.port}")
	private Integer cerebrumPort;
	
	@Value("${cerebrum.host.path}")
	private String cerebrumHostPath;
	
	/**
	 * Constructor used by the <b>Spring container</b> to create a {@link SynapseBootstraper} object. This object is managed by the <b>Spring container</b> and is singleton scoped. 
	 * This object is then injected into the Synapse application via the Spring container.
	 * 
	 * @param reactorCluster
	 * @return 
	 */
	public SynapseBootstraper(ReactorCluster reactorCluster)
	{
		System.out.println("Bootstraping in progress .......");
		// 1. Get the SynapticReactorCluster (singleton scoped)
		this.reactorCluster = reactorCluster;
	}
	
	public void boot() throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException, ConnectionException, ImproperConnectorInstantiationException, SSLContextInitializationException
	{
		DocumentStore.initiate();
		IdGenerator.initiate();
		NcephLogger.BOOTSTRAP_LOGGER.info("Initializing " + SynapticIncomingMessageType.types.length + " incoming message types");
		NcephLogger.BOOTSTRAP_LOGGER.info("Initializing " + SynapticOutgoingMessageType.types.length + " outgoing message types");
		
		// 1. Create a synaptic connector
		SynapticConnector connector = new SynapticConnector.Builder()
				.name(connectorName) 
				.port(cerebrumPort) // Should pick from local project configuration where the synapse is installed
				.hostPath(cerebrumHostPath)
				.readerPool(new WorkerPool.Builder<Reader>()
						.corePoolSize(10)
						.maximumPoolSize(100)
						.keepAliveTime(60)
						.workQueue(new LinkedBlockingQueue<Runnable>())
						.rejectedThreadHandler(new RejectedReaderHandler())
						.build())
				.writerPool(new WorkerPool.Builder<Writer>()
						.corePoolSize(10)
						.maximumPoolSize(100)
						.keepAliveTime(60)
						.workQueue(new LinkedBlockingQueue<Runnable>())
						.rejectedThreadHandler(new RejectedWriterHandler())
						.build())
				.sslContext(NcephSSLContext.getSSLContext())
				.build();
		
		
		// 2. Instantiate the singleton Emitter object
		Emitter.initiate(connector);
		
		
		
		// 3. Run the reactors
		reactorCluster.run();
		/*
		System.out.println("-----------------------");
		connector.getConnection().teardown();
		System.out.println("-----------------------");
		
		connector.connect();*/
		// 4. Render the menu to monitor the running of Synapse
		Scanner input = new Scanner(System.in);
		int choice;
		menuLoop: while(true)
	    {
			System.out.println("Menu");
	    	System.out.println("1.) Synapse status");
	    	System.out.println("2.) Publish_Event");
	    	System.out.println("3.) Connector Status");
	    	System.out.println("4.) Shutdown.");
	    	System.out.println("\nEnter Your Menu Choice: ");
	    	
	    	choice = input.nextInt();
	    	
	    	switch (choice) {
			case 1:
				// Get the messages directory
	    		File messageDirectory = new File(Configuration.APPLICATION_PROPERTIES.getConfig("document.localStore.published_location"));
	    		// If there are no pods the print the no pod found message
	    		System.out.println(messageDirectory.listFiles());
	    		if(messageDirectory.listFiles() == null)
	    		{
	    			Console.error("POD's not found in Synapse");
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
					System.out.println("Selected event is "+EventType.types[eventChoice-1].getGiftClass().getSimpleName());
					System.out.print("How many messages do you want to publish: ");
					numberOfEvents = input.nextInt();
					EventThread thread = new EventThread.Builder()
							.numberOfEvents(Integer.valueOf(numberOfEvents))
							.implementationClass(EventType.types[eventChoice-1].getCallingclass())
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
			case 3:
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
    					connector.getReaderPool().getActiveWorkers().intValue()+ connector.getWriterPool().getActiveWorkers().intValue(),
    					connector.getReaderPool().getTotalWorkersCreated().intValue()+ connector.getWriterPool().getTotalWorkersCreated().intValue(),
    					connector.getReaderPool().getTotalSuccessfulWorkers().intValue()+ connector.getWriterPool().getTotalSuccessfulWorkers().intValue(),
    					connector.getRelayQueue().size()
    					);
    			System.out.println(" _________________________________________________________________________________________________________________________________________________");
    			// 3.2 This table shows connector's registers status
    			System.out.println("\nConnector Status 2");
    			System.out.println(" ________________________________________________________________________________________________________________________");
    			System.out.printf("| %6s | %20s | %20s | %30s | %30s | \n",
    					"PORT",
    					"Incoming_Register",
    					"Outgoing_Register",
    					"Connector_Queued_Up_Register",
    					"Connection_Queued_Up_Register"
    					);
    			System.out.println(" ------------------------------------------------------------------------------------------------------------------------");
    			System.out.printf("| %6d | %20d | %20d | %30d | %30s | \n",
    					connector.getPort(),
    					connector.getIncomingMessageRegister().size(),
    					connector.getOutgoingMessageRegister().size(),
    					connector.getConnectorQueuedUpMessageRegister().size(),
    					connector.getConnectionQueuedUpMessageRegister().size()
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
			case 4:
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
