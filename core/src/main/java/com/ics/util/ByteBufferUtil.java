package com.ics.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * 
 * @author Anurag Arya
 * @version 1.0
 * @since 24-Dec-2021
 */
public class ByteBufferUtil 
{
	/**
	 * 
	 * 
	 * @param byteBuffer
	 * @throws Exception
	 * @return Object
	 */
	public static Object byteBufferToObject(ByteBuffer byteBuffer) throws Exception 
	{
		byte[] bytes = new byte[byteBuffer.limit()];
		byteBuffer.get(bytes);
		Object object = deserializer(bytes);
		return object;
	}

	/**
	 * 
	 * 
	 * @param bytes
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return Object
	 */
	public static Object deserializer(byte[] bytes) throws IOException, ClassNotFoundException 
	{
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
		return objectInputStream.readObject();
	}

	/**
	 * 
	 * 
	 * @param obj
	 * @throws IOException
	 * @return ByteBuffer
	 */
	public static ByteBuffer toBuffer(Serializable obj) throws IOException 
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(obj);
		oos.flush();
		ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
		oos.close();
		baos.close();
		return buffer;
	}
}
