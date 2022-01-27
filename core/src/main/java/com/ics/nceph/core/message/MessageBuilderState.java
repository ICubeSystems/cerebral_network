package com.ics.nceph.core.message;

/**
 * States of {@link MessageBuilder} instance: <br>
 * <ol>
 * 	<li> 1. Pending read - Newly created MessageBuilder instance is in initiated state or when the next genesis byte is encountered after a successful message building
 *  <li> 2. Header assembled - Once the header is fully assembled. If the receiving buffer has less than 16 bytes remaining after the genesis byte then the bytes are held in temporary bytes instead of message bytes
 *  <li> 3. Body assembled - Once the full body is received. If the receiving buffer has less bytes than the length of the message then the data bytes are held in the temporary bytes instead of body bytes on the message 
 * </ol>
 * @author Anurag Arya
 * @version 1.0
 * @since 01-Jan-2022
 */
public class MessageBuilderState 
{
	private int state;
	
	MessageBuilderState(int state)
	{
		this.state = state;
	}
	
	public int getValue() {
		return state;
	}
	
	/**
	 * Pending read - Newly created MessageBuilder instance is in initiated state or when the current Message object is created
	 */
	public static MessageBuilderState INITIATED = new MessageBuilderState(100);
	
	/**
	 * Pending read - When the next genesis byte is encountered
	 */
	public static MessageBuilderState READ_STARTED = new MessageBuilderState(200);
	
	/**
	 * Header assembled - Once the header is fully assembled. If the receiving buffer has less than 16 bytes remaining after the genesis byte then the bytes are held in temporary bytes instead of message bytes
	 */
	public static MessageBuilderState HEADER_ASSEMBLED = new MessageBuilderState(300);
	
	/**
	 * Body assembled - Once the full body is received. If the receiving buffer has less bytes than the length of the message then the data bytes are held in the temporary bytes instead of body bytes on the message 
	 */
	public static MessageBuilderState BODY_ASSEMBLED = new MessageBuilderState(400);
}
