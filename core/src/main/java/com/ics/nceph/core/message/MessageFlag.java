package com.ics.nceph.core.message;

import java.util.BitSet;

/**
 * Message flags are allocated 1 byte in the {@link Message}. They are stored using the {@link BitSet} instance (8 bits).
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 31-Dec-2021
 */
public class MessageFlag 
{
	/**
	 * The position start from right, the right most element in the bitset starts with 0
	 */
	private int position;
	
	MessageFlag(int position)
	{
		this.position = position;
	}
	
	public int getPosition() {
		return position;
	}

	/**
	 * 
	 */
	public static MessageFlag TRACE_FLAG = new MessageFlag(0);
}
