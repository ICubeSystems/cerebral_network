package com.ics.synapse.applicationReceptor;

import java.lang.reflect.InvocationTargetException;

import com.ics.logger.GeneralLog;
import com.ics.logger.LogData;
import com.ics.logger.NcephLogger;
import com.ics.nceph.config.ConfigStore;
import com.ics.nceph.core.connector.connection.Connection;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.message.Message;
import com.ics.nceph.core.worker.Reader;
import com.ics.synapse.applicationReceptor.exception.ApplicationReceptorFailedException;

/**
 * This class is responsible for processing the incoming message inside a reader (worker) thread. 
 * {@link Connection#read()} reads the {@link Message} and then starts a {@link Reader} thread to process the incoming message. 
 * The {@link Reader} instantiates <b>Receptor</b> based on MessageType
 * 
 * @author Anshul
 * @version 1.0
 * @since Aug 3, 2022
 */
public abstract class ApplicationReceptor 
{
	EventData eventData;

	/**
	 * Abstract method to be implemented by all the Application Receptor classes
	 * 
	 * @return void
	 */
	abstract public void process() throws Exception;

	public void execute() throws ApplicationReceptorFailedException
	{
		try {
			process();
		} catch (Exception e) {
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

	public ApplicationReceptor(EventData eventData)
	{
		this.eventData = eventData;
	}

	public EventData getEventData() {
		return eventData;
	}

	public static class Builder
	{
		EventData eventData;

		Class<? extends ApplicationReceptor> implementationClass;

		public Builder eventData(EventData eventData) {
			this.eventData = eventData;
			return this;
		}

		public ApplicationReceptor build() throws ApplicationReceptorFailedException
		{
			try {
				// Class load the Receptor object
				Class<?>[] constructorParamTypes = {EventData.class};
				Object[] params = {eventData};
				return (ApplicationReceptor) Class.forName(ConfigStore.getApplicationReceptor(eventData.getEventType())).getConstructor(constructorParamTypes).newInstance(params);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				throw new ApplicationReceptorFailedException(e.getLocalizedMessage(), e);
			}
		}
	}
}
