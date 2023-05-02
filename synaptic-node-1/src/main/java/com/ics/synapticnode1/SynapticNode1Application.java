package com.ics.synapticnode1;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import com.egiftify.events.ActorInfo;
import com.egiftify.events.RequestInfo;
import com.egiftify.events.order.FraudAlertEvent;
import com.ics.synapse.Emitter;
import com.ics.synapse.bootstrap.SynapseBootstraper;

@SpringBootApplication
@ImportResource("classpath:synapse_context.xml")
public class SynapticNode1Application implements CommandLineRunner
{
	@Autowired
	private SynapseBootstraper synapseBootstraper;
	
	public static void main(String[] args) 
	{
		SpringApplication synapse = new SpringApplication(SynapticNode1Application.class);
		synapse.setBannerMode(Banner.Mode.OFF);
		// @todo - Create Banner for the project and set it here for logging - use the Banner class provided by Spring.
		synapse.run(args);
	}
	
	@Override
	public void run(String... args) throws Exception 
	{
		// 1. Start the connector
		synapseBootstraper.boot();
		for (int i = 0; i < 200; i++)
		{
			FraudAlertEvent event = FraudAlertEvent.builder()
					.actorId(ActorInfo.builder()
							.actorId("anshul" + i)
							.build())
					.requestInfo(RequestInfo.builder()
							.time(Date.from(LocalDateTime.of(2000+i, 2, 25, 1, 20).atZone(ZoneId.systemDefault()).toInstant()))
							.build())
					.orderNumber("1234"+i)
					.build();
			Emitter.emit(event.toEvent(null));
		}
		
		

	}
}
