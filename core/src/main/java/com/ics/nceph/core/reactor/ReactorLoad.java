package com.ics.nceph.core.reactor;

import java.nio.channels.SelectionKey;
import java.util.Objects;

/**
 * Instances of this class are entered in the reactor load balancer. Every alive reactor is attached with a single ReactorLoad object. ReactorLoad object is created and destroyed 
 * along with the reactor it is attached to. It acts like a container class containing following information:
 * 1. activeKeys - number of active {@link SelectionKey} instances (only for {@link SelectionKey#OP_READ} & {@link SelectionKey#OP_WRITE} operations) this reactor is listening to
 * 2. reactorId - id of the reactor this object is attached to
 * 3. totalKeysServed - total number of {@link SelectionKey} instances served by this reactor in its lifetime
 * 
 * This class implements Comparable for min-heap implementation of the reactor load balancer (priority queue implementation)
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 21-Dec-2021
 */
public class ReactorLoad implements Comparable<ReactorLoad> 
{
	private int activeKeys;

	private int reactorId;
	
	private int totalKeysServed = 0;

	ReactorLoad(int reactorId, int activeKeys)
	{
		this.reactorId = reactorId;
		this.activeKeys = activeKeys;
	}
	
	
	@Override
	public int compareTo(ReactorLoad reactorLoad) 
	{
		if(getActiveKeys() > reactorLoad.getActiveKeys() // if activeKeys is greater then return 1
				|| (getActiveKeys() == reactorLoad.getActiveKeys() && getTotalKeysServed() > reactorLoad.getTotalKeysServed())) // if activeKeys is same and totalKeysServed is greater then return 1 
			return 1;
		else if (getActiveKeys() < reactorLoad.getActiveKeys() 
				|| (getActiveKeys() == reactorLoad.getActiveKeys() && getTotalKeysServed() < reactorLoad.getTotalKeysServed())) 
			return -1;
		else 
			return 0;
	}
	
	@Override
	public String toString() 
	{
		return "ReactorLoad{" +
				"reactorId=" + reactorId + 
				", activeKeys=" + activeKeys +
				", totalKeysServed=" + totalKeysServed +
				'}';
	}

    @Override
    public boolean equals(Object o) 
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactorLoad reactorLoad = (ReactorLoad) o;
        return Integer.compare(reactorLoad.activeKeys, activeKeys) == 0 &&
        		Integer.compare(reactorLoad.reactorId, reactorId) == 0 &&
        		Integer.compare(reactorLoad.totalKeysServed, totalKeysServed) == 0;
    }

    @Override
    public int hashCode() 
    {
        return Objects.hash(reactorId, activeKeys, totalKeysServed);
    }

    public int getActiveKeys() {
		return activeKeys;
	}

	public void setActiveKeys(int activeKeys) {
		this.activeKeys = activeKeys;
	}

	public int getReactorId() {
		return reactorId;
	}

	public void setReactorId(int reactorId) {
		this.reactorId = reactorId;
	}

	public int getTotalKeysServed() {
		return totalKeysServed;
	}

	public void setTotalKeysServed(int totalKeysServed) {
		this.totalKeysServed = totalKeysServed;
	}
}
