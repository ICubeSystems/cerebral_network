package com.ics.cerebrum;

import java.io.File;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import com.ics.cerebrum.bootstrap.Bootstraper;
import com.ics.console.Console;
import com.ics.nceph.core.Configuration;
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
	    	System.out.print("1.) Cerebral status \n");
	    	System.out.print("2.) Shutdown.\n");
	    	System.out.print("\nEnter Your Menu Choice: ");

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
	    		int totalPods = messageDirectory.listFiles().length;
	    		int invalidPods = 0;
	    		for (File podFile : messageDirectory.listFiles()) 
	    		{
	    			ProofOfDelivery pod = DocumentStore.load(podFile);
	    			// Print the validity status of the POD
	    			String status = pod.validate();
	    			if(pod.validate()!="") {
	    				Console.error(podFile.getName()+" ✕ "+status);
	    				invalidPods++;
	    			}
	    			else
	    				Console.success(podFile.getName()+" ✓ ");
	    		}
	    		Console.success("Total pods : " + totalPods);
	    		Console.error("Invalid pods: "+invalidPods);
	    		break;
	    	case 2:
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
