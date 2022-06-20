package com.ics.synapse.ncephEvent;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.ics.nceph.core.event.EventData;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 01-Mar-2022
 */
public interface NcephEvent 
{
	 EventData toEvent() throws JsonProcessingException, IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException;
}
