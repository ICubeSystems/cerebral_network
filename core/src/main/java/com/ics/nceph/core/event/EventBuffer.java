package com.ics.nceph.core.event;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.ics.nceph.core.event.exception.ImproperEventBufferInstantiationException;

/**
 * Class encapsulating ByteBuffer instance and some utility methods. <br>
 * EventBuffer are used for the following purpose:<br>
	     * <ol>
	     * 		<li>Reading the event from the socket channel</li>
	     * 		<li>Writing an event to a socket channel</li>
	     * </ol>
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 24-Dec-2021
 */
public class EventBuffer 
{
	ByteBuffer buffer;
	
	EventBuffer(ByteBuffer buffer)
	{
		this.buffer = buffer;
		buffer.clear();
	}
	
	EventBuffer(Event event) throws IOException
	{
		toBuffer(event);
	}
	
	/**
	 * Return the Event instance from the ByteBuffer
	 * 
	 * @return Event
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public Event toEvent() throws ClassNotFoundException, IOException 
	{
		buffer.flip();
		return (Event)toObject();
	}
	
	/**
	 * Return the Object from the ByteBuffer
	 * 
	 * @return Object
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
	public Object toObject() throws IOException, ClassNotFoundException
	{
		byte[] bytes = new byte[buffer.limit()];
		buffer.get(bytes);
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
		return objectInputStream.readObject();
	}
	
	/**
	 * Creates a new {@link ByteBuffer} instance and puts the event object into it.
     * The new buffer's capacity and limit will be {@code array.length}, its position will be zero, its mark will be undefined, and its byte order will be
     * {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.
     * 
	 * @param object
	 * @throws IOException
	 * @return ByteBuffer
	 */
	public void toBuffer(Serializable object) throws IOException 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(object);
		oos.flush();
		buffer = ByteBuffer.wrap(baos.toByteArray());
		//System.out.println("Wrapped buffer size: "+buffer.capacity());
		oos.close();
		baos.close();
	}

	/**
	 * Return the ByteBuffer
	 * 
	 * @return ByteBuffer
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}
	
	/**
	 * Builds the {@link EventBuffer} instance
	 * 
	 * @throws IOException
	 * @return Connector
	 */
	public static class Builder
	{
		int capacity = 0;
		
		Event event;
		
		/**
		 * Set the capacity of the new {@link ByteBuffer}, in bytes
		 * 
		 * @param capacity - The new buffer's capacity, in bytes
		 * @return Builder
		 */
		public Builder capacity(int capacity) {
			this.capacity = capacity;
			return this;
		}
		
		/**
		 * Set the {@link Event} instance to be converted to the ByteBuffer for sending on the socket channel
		 * 
		 * @param event
		 * @return Builder
		 */
		public Builder event(Event event) {
			this.event = event;
			return this;
		}
		
		/**
	     * Creates a new EventBuffer instance. EventBuffer are used for the following purpose:<br>
	     * <ol>
	     * 		<li>Reading the event from the socket channel - in this case, {@link EventBuffer.Builder#event(Event)} should not be called during the build. 
	     * 			The new buffer's position will be zero, its limit will be its capacity its mark will be undefined, 
	     * 			each of its elements will be initialized to zero, and its byte order will be {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN}.
	     * 		<li>Writing an event to a socket channel - in this case {@link EventBuffer.Builder#capacity(int)} should not be called during the build.</li>
	     * </ol>
	     * 
	     * @return  The new EventBuffer instance containing the ByteBuffer and some utility methods
		 * @throws IOException 
		 * @throws ImproperEventBufferInstantiationException 
	     * @throws IllegalArgumentException - If the {@code capacity} is a negative integer
	     */
		public EventBuffer build() throws IOException, ImproperEventBufferInstantiationException
		{
			// Throw an ImproperEventBufferInstantiationException if both event and capacity are set before the build call 
			if (event != null && capacity != 0)
				throw new ImproperEventBufferInstantiationException(new Exception("Can not instantiate EventBuffer with Event and Capacity"));
			
			// If the buffer is being created for writing on the socket channel
			if (event != null)
				return new EventBuffer(event);
			else // If the buffer is being created for reading from the socket channel
			{
				ByteBuffer buffer = ByteBuffer.allocate(capacity);
				return new EventBuffer(buffer);
			}
		}
	}
}
