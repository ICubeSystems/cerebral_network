package com.ics.cerebrum;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

import com.ics.cerebrum.bootstrap.Bootstraper;
import com.ics.cerebrum.menu.CerebralMenu;


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

		// 2. Run cerebral menu
		CerebralMenu.run();
	}
}
