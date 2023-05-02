package com.ics.synapse.ncephEvent;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ics.nceph.core.event.EventData;
import com.ics.synapse.exception.EventDataException;
import com.ics.synapse.ncephEvent.exception.EventValidationException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Base class for all events in the eGiftify cerebral network
 * 
 * @author Anshul
 * @version 1.0.1
 * @since Dec 20, 2022
 */

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public abstract class Event implements NcephEvent, Serializable
{
	private static final long serialVersionUID = -3996054630188029806L;
	
	/**
	 * 
	 * @author Anshul
	 * @return
	 */
	@NotNull(message = "Event type can not be null")
	public abstract Integer getType();
	
	/**
	 * This method programmatically executes all the validation annotations defined on the Event object
	 * 
	 * @author Anshul
	 * @version V_1_0_2
	 * @since Apr 27, 2023
	 * @throws EventDataException
	 */
	public void validate() throws EventDataException
	{
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<Event>> violations = validator.validate(this);
		if(!violations.isEmpty())
			throw EventValidationException.raise(violations);
	}

	@Override
	public EventData toEvent(Integer eventId) throws EventDataException 
	{
		try
		{
			//1. Validate the event object (use validation annotations instead of manual validations)
			validate();
			//2. Build the EventData to emit on the network
			return new EventData.Builder()
					.createdOn(new Date().getTime())
					.eventId(eventId)
					.eventType(getType())
					.objectJSON(toJSON())
					.build();
		} 
		catch (JsonProcessingException e)
		{
			e.printStackTrace();
			// ERROR LOG
			throw new EventDataException("Event JSON parsing failed: Error in processing JSON of the event: " + e.getMessage(), e);
		} 
		catch (EventDataException e)
		{
			// ERROR LOG
			throw e;
		}
	}
	
	private String toJSON() throws JsonProcessingException 
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}
}
