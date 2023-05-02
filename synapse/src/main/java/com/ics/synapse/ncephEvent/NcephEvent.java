package com.ics.synapse.ncephEvent;

import com.ics.nceph.core.event.EventData;
import com.ics.synapse.exception.EventDataException;
/**
 * 
 * @author Anshul
 * @author Anurag Arya
 * @version 1.0
 * @since 01-Mar-2022
 */
public interface NcephEvent 
{
	 EventData toEvent(Integer eventId) throws EventDataException;
}
