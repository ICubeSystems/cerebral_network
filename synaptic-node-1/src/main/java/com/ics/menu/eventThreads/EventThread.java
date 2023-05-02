package com.ics.menu.eventThreads;

import java.lang.reflect.InvocationTargetException;

public class EventThread extends Thread {

	Integer numberOfEvents;
	
	public EventThread(Integer totalEvents){
		this.numberOfEvents = totalEvents;
	}
	
	public int getNumberOfEvents() {
		return numberOfEvents;
	}
	public void run() {
		
	}
	public static class Builder
	{
		Integer numberOfEvents;
				
		Class<? extends EventThread> implementationClass;
		
		public Builder numberOfEvents(Integer numberOfEvents) {
			this.numberOfEvents = numberOfEvents;
			return this;
		}
		
		public Builder implementationClass(Class<? extends EventThread> implementationClass) {
			this.implementationClass = implementationClass;
			return this;
		}
		
		public EventThread build()throws EventThreadInstantiationException
		{
			
				// Class load the Receptor object
				Class<?>[] constructorParamTypes = {Integer.class};
				Object[] params = {numberOfEvents};
				
				// Return Receptor
				try {
					return implementationClass.getConstructor(constructorParamTypes).newInstance(params);
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
					// TODO Auto-generated catch block
					throw new EventThreadInstantiationException("EventThread instantiation exception", e);
				}
			
		}
	}
}
