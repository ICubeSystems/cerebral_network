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
		
		for (int i = 1; i <= 100; i++)
		{
			try
			{
				GiftItem gi = new GiftItem.Builder()
						.recipientName("Receiver " + i)
						.senderName("Sender " + i)
						.giftCode("570087975676400" + i)
						.amount(new BigDecimal(100.0))
						.balance(new BigDecimal(100.0))
						.build();

				Emitter.emit(gi.toEvent());
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		
////		Create GiftItem
//		GiftItemThread createGift = new GiftItemThread();
//		createGift.start();
//		
//////		Redeem Gift
//		GiftRedeemThread redeemGift = new GiftRedeemThread();
//		redeemGift.start();
//		
//////		Refund Gift
//		GiftRefundThread refundGift = new GiftRefundThread();
//		refundGift.start();
		

	}
}
