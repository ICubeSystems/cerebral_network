package com.ics.synapticnode1;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

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
		// 2. Present the menu options
		for (int i = 0; i < 1000; i++) {
			GiftItem gi = new GiftItem.Builder()
					.recipientName("Anurag Arya")
					.senderName("Ragini Arya")
					.giftCode("5700879756764435")
					.amount(new BigDecimal(100.00))
					.balance(new BigDecimal(100.00))
					.build();
			Emitter.emit(gi.toEvent());
		}
	}
}