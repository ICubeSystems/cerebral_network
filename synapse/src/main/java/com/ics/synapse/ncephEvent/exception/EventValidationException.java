package com.ics.synapse.ncephEvent.exception;

import java.util.Set;
import java.util.StringJoiner;

import com.ics.synapse.exception.EventDataException;
import com.ics.synapse.ncephEvent.Event;

import jakarta.validation.ConstraintViolation;

/**
 * This is an exception class for handling event object validation exceptions
 * 
 * @author Anshul
 * @version 1.0.2
 * @since Apr 27, 2023
 */
public class EventValidationException
{
	public static EventDataException raise(Set<ConstraintViolation<Event>> violations ) 
	{
		StringJoiner joiner = new StringJoiner(", ");
		for(ConstraintViolation<Event> violation : violations) {
			joiner.add(violation.getMessage());
		}
		return new EventDataException(joiner.toString());
	}
}
