package com.ics.cerebrum.menu;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ics.cerebrum.connector.CerebralConnector;
import com.ics.nceph.NcephConstants;
import com.ics.nceph.core.connector.Connector;
import com.ics.nceph.core.connector.ConnectorCluster;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.db.document.ProofOfPublish;
import com.ics.nceph.core.db.document.ProofOfRelay;
import com.ics.nceph.core.db.document.store.cache.ApplicationMessageCache;
import com.ics.nceph.core.db.document.store.cache.DocumentCache;
import com.ics.nceph.core.db.document.store.cache.MessageCache;
import com.ics.nceph.core.message.MessageLedger;

/**
 * Menu for cerebrum
 * @author Anshul
 * @version 1.0
 * @since Sep 30, 2022
 */
public class CerebralMenu
{
	public static void run() {
		// 2. Render the menu to monitor the running of Encephalon
				Scanner input = new Scanner(System.in);
				int choice;
				menuLoop: while(true)
				{
					// Display menu graphics
				    System.out.println("================================");
				    System.out.println("|        MENU SELECTION        |");
				    System.out.println("================================");
				    System.out.println("| Options:                     |");
				    System.out.println("|         1. Connector         |");
				    System.out.println("|         2. TLS_MODE          |");
				    System.out.println("|         3. Shutdown          |");
				    System.out.println("================================");
					System.out.println("\nEnter Your Menu Choice: ");

					choice = input.nextInt();
					switch(choice)
					{
					case 1:
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
							System.out.printf("| %6s | %20s | %20s | %30s | %30s | %20s | \n",
									"PORT",
									"Incoming_Register",
									"Outgoing_Register",
									"Connector_Queued_Up_Register",
									"Connection_Queued_Up_Register",
									"Publish Cache size"
									);

							System.out.printf("| %6d | %20d | %20d | %30d | %30s | %20d \n",
									connector.getPort(),
									connector.getIncomingMessageRegister().size(),
									connector.getOutgoingMessageRegister().size(),
									connector.getConnectorQueuedUpMessageRegister().size(),
									connector.getConnectionQueuedUpMessageRegister().size(),
									ProofOfPublish.getMessageCache(connector.getPort()) != null ? ProofOfPublish.getMessageCache(connector.getPort()).size() : 0
									);
									
							ApplicationMessageCache<ProofOfRelay> relayCache = DocumentCache.getInstance().getRelayedMessageCache().get(connector.getPort());
									if(relayCache != null) {
									System.out.println("\n Relay Cache ");
									System.out.printf("| %12s | %10s | \n",
											"Port Number",
											"Count"
											);
									
									for (Map.Entry<Integer, MessageCache<ProofOfRelay>> entry : relayCache.entrySet())
									{
//										System.out.println(entry.getKey()+"--"+entry.getValue());
										System.out.printf("| %12d | %10d | \n",
												entry.getKey(),
												entry.getValue().size()
												);
									}
									}
							
							
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
										connection.getMetric().getActiveRequests().intValue(),
										connection.getMetric().getTotalRequestsServed().intValue(),
										connection.getMetric().getTotalSuccessfulRequestsServed().intValue(),
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
					case 2:
						System.out.println("TLS_MODE is set to "+NcephConstants.TLS_MODE);
						break;
					case 3:
						System.out.println("COMING SOON....");
						input.close();
						break menuLoop;
					default:
						System.out.println("Invalid selection....");
					}
				}
	}
}
