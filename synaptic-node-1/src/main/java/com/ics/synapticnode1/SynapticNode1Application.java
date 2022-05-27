package com.ics.synapticnode1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import com.ics.synapse.bootstrap.SynapseBootstraper;
import com.ics.synapticNode1Thread.GiftItemThread;
import com.ics.synapticNode1Thread.GiftRedeemThread;
import com.ics.synapticNode1Thread.GiftRefundThread;

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
		
//		Create GiftItem
		GiftItemThread createGift = new GiftItemThread();
		createGift.start();
		
//		Redeem Gift
		GiftRedeemThread redeemGift = new GiftRedeemThread();
		redeemGift.start();
		
//		Refund Gift
		GiftRefundThread refundGift = new GiftRefundThread();
		refundGift.start();
		
	}
}
