package com.ics.synapse.applicationReceptor;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.ics.logger.GeneralLog;
import com.ics.logger.LogData;
import com.ics.logger.NcephLogger;
import com.ics.nceph.core.db.document.store.ConfigStore;
import com.ics.nceph.core.event.EventData;
import com.ics.synapse.applicationReceptor.exception.ApplicationReceptorFailedException;
import com.ics.synapse.ncephEvent.Event;
import com.ics.synapse.receptor.RelayedEventReceptor;

/**
 * Application receptor is instantiated by {@link RelayedEventReceptor} whenever a synaptic node receives a subscribed {@link Event} (message). 
 * This is a generic contract class for all the application receptors within an application.
 * 
 * @author Anshul
 * @version 1.0
 * @since Aug 3, 2022
 */
public abstract class ApplicationReceptor<T extends Event>
{
	/**
	 * Raw data of the event received by the ApplicationReceptor (transmitted by cerebrum)
	 */
	private EventData eventData;
	
	/**
	 * Object of {@link Event} for which the receptor is being instantiated (called). This is constructed from {@link EventData#getObjectJSON()}
	 */
	private T eventObject; 

	/**
	 * Abstract method to be implemented by all extending application receptor classes to process the event reception. 
	 * Application business logic for the event should be encapsulated in this method's implementation.
	 * 
	 * @throws Exception
	 * @return Void
	 */
	public abstract void process() throws Exception;

	/**
	 * Abstract method to be implemented by all extending application receptor classes to perform any processing required before processing the actual business logic. 
	 * This method is called before the {@link #process()} method is executed.<Br>
	 * A good example of this method implementation is to create a base receptor class in the application and implement this method to handle the auditing requirements of the auditable events. 
	 * So each application receptor in the application then do not need to implement this method to handle auditing.
	 * 
	 * @since Apr 26, 2023
	 * @return Void
	 */
	public abstract void preProcess();
	
	/**
	 * Abstract method to be implemented by all extending application receptor classes to perform any processing required after processing the actual business logic. 
	 * This method is called after the {@link #process()} method is executed.<Br>
	 * A good example of this method implementation is to create a base receptor class in the application and implement this method to handle the auditing requirements of the auditable events. 
	 * So each application receptor in the application then do not need to implement this method to handle auditing.
	 * 
	 * @since Apr 26, 2023
	 * @return Void
	 */
	public abstract void postProcess();
	
	/**
	 * After the instantiation of ApplicationReceptor object is done, this method is called to start the processing of the received event.
	 * 
	 * @throws ApplicationReceptorFailedException
	 * @return Void
	 */
	public void execute() throws ApplicationReceptorFailedException
	{
		try 
		{
			// Execute pre processing implementation
			preProcess();
			// Execute the business logic
			process();
			// Execute the post processing implementation 
			postProcess();
		} catch (Exception e) 
		{
			NcephLogger.GENERAL_LOGGER.error(new GeneralLog.Builder()
					.action("NCEPH_EVENT_ACK build failed")
					.data(new LogData()
							.entry("EventId", String.valueOf(eventData.getEventId()))
							.entry("EventType", String.valueOf(eventData.getEventType()))
							.entry("Error", e.getMessage())
							.toString())
					.logError(),e);
			throw new ApplicationReceptorFailedException(e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Constructor used by the builder to construct the ApplicationReceptor object
	 * 
	 * @param eventData - Raw data of the event received by the ApplicationReceptor (transmitted by cerebrum)
	 * @param eventObject - Object of {@link Event} for which the receptor is being instantiated (called). This is constructed from {@link EventData#getObjectJSON()}
	 */
	protected ApplicationReceptor(EventData eventData, T eventObject)
	{
		this.eventData = eventData;
		this.eventObject = eventObject;
	}
	

	public EventData getEventData() {
		return eventData;
	}

	public T getEventObject()
	{
		return eventObject;
	}
	
	/**
	 * Builder to build ApplicationReceptor object by eventData
	 * @param <T>
	 */
	public static class Builder<T extends Event>
	{
		EventData eventData;
		
		T eventObject;
		
		protected final ObjectMapper builderMapper = new ObjectMapper()
				.setConstructorDetector(ConstructorDetector.USE_DELEGATING)
				.enable(SerializationFeature.INDENT_OUTPUT)
				.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm a z"))
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.setSerializationInclusion(Include.NON_NULL);

		public Builder<T> eventData(EventData eventData) {
			this.eventData = eventData;
			return this;
		}
		
		public ApplicationReceptor<? extends Event> build() throws ApplicationReceptorFailedException
		{
			if (eventData == null)
				throw new ApplicationReceptorFailedException("Cannot instantiate ApplicationReceptor without eventData");
			try 
			{
				// Get eventObject class for the event type
				@SuppressWarnings("unchecked")
				Class<T> eventObjectClass = (Class<T>) Class.forName(ConfigStore.getInstance().getEventClass(eventData.getEventType()));
				
				// Get ApplicationReceptor class for the event type
				@SuppressWarnings("unchecked")
				Class<? extends ApplicationReceptor<T>> applicationReceptorClass = (Class<? extends ApplicationReceptor<T>>) Class.forName(ConfigStore.getInstance().getApplicationReceptor(eventData.getEventType()));
				
				// Convert eventData.getObjectJSON() to appropriate eventObject
				eventObject = builderMapper.readValue(eventData.getObjectJSON(), eventObjectClass);
				
				// Create ApplicationReceptor
				Class<?>[] constructorParamTypes = {EventData.class, eventObject.getClass()};
				Object[] params = {eventData, eventObject};
				return (ApplicationReceptor<?>) applicationReceptorClass.getConstructor(constructorParamTypes).newInstance(params);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException | JsonProcessingException e) {
				throw new ApplicationReceptorFailedException(e.getLocalizedMessage(), e);
			}
			catch(NullPointerException npe) {
				throw new ApplicationReceptorFailedException("Invalid application receptor metadata", npe);
			}
		}
	}
}