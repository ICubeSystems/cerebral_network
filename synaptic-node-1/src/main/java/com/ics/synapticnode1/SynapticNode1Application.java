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
		GiftItem gi = new GiftItem.Builder()
				.recipientName("Anurag Arya")
				.senderName("Ragini Arya")
				.giftCode("5700879756764435")
				.amount(new BigDecimal(100.00))
				.balance(new BigDecimal(100.00))
				.build();
		GiftItem gi1 = new GiftItem.Builder()
				.recipientName("Anshul")
				.senderName("Chandan")
				.giftCode("5700879756764435")
				.amount(new BigDecimal(100.00))
				.balance(new BigDecimal(100.00))
				.build();
		GiftItem gi2 = new GiftItem.Builder()
				.recipientName("Viren")
				.senderName("Ragini Arya")
				.giftCode("5700879756764435")
				.amount(new BigDecimal(100.00))
				.balance(new BigDecimal(100.00))
				.build();
		GiftItem gi3 = new GiftItem.Builder()
				.recipientName("Gunja")
				.senderName("Ragini Arya")
				.giftCode("5700879756764435")
				.amount(new BigDecimal(100.00))
				.balance(new BigDecimal(100.00))
				.build();
		GiftItem gi4= new GiftItem.Builder()
				.recipientName("Prashant")
				.senderName("Ragini Arya")
				.giftCode("5700879756764435")
				.amount(new BigDecimal(100.00))
				.balance(new BigDecimal(100.00))
				.build();
		GiftItem gi5 = new GiftItem.Builder()
				.recipientName("Anurag Arya")
				.senderName("Yash")
				.giftCode("5700879756764435")
				.amount(new BigDecimal(100.00))
				.balance(new BigDecimal(100.00))
				.build();
		
		Emitter.emit(gi.toEvent());
		Emitter.emit(gi1.toEvent());
		Emitter.emit(gi2.toEvent());
		Emitter.emit(gi3.toEvent());
		Emitter.emit(gi5.toEvent());
		Emitter.emit(gi4.toEvent());
	}
}