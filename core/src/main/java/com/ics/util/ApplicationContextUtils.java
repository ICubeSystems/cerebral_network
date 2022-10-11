package com.ics.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Used to get objects from spring factory
 * @author Anshul
 * @version 1.0
 * @since Aug 31, 2022
 */
@Component
public class ApplicationContextUtils implements ApplicationContextAware
{
	public static ApplicationContext context;
   
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
	{
		context = applicationContext;
	}
}
