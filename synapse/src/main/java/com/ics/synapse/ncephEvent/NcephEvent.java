package com.ics.synapse.ncephEvent;

import java.io.IOException;

import com.ics.nceph.core.event.EventData;
/**
 * 
 * @author Chandan Verma
 * @version 1.0
 * @since 01-Mar-2022
 */
public interface NcephEvent 
{
	 EventData toEvent() throws IOException;
}
