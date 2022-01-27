package com.ics.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;

public class ByteUtil 
{
	/**
	 * 
	 * 
	 * @param Int to be converted to 
	 * @return byte[]
	 */
	public static byte[] convertToByteArray(long num, int byteCount)
	{
		byte[] result = new byte[byteCount];
		int shiftMultiplier = byteCount-1;
		for (int i = 0; i < byteCount; i++) 
		{
			result[i] = (byte) (num >> 8*shiftMultiplier);
			shiftMultiplier--;
		}
		return result;
	}
	
	/**
	 * 
	 * 
	 * @param bytes
	 * @return long
	 */
	public static int convertToInt(byte[] bytes) 
	{
		//if (bytes.length > 4)
		//	throw InvalidConversionException 
		int number=0;
		for (byte b : bytes) 
			number = (number << 8) + (b & 0xFF);
	    return number;
	 }
	
	/**
	 * 
	 * 
	 * @param byte
	 * @return long
	 */
	public static int convertToInt(byte b) 
	{
		return (0 << 8) + (b & 0xFF);
	}
	
	/**
	 * 
	 * 
	 * @param bytes
	 * @return long
	 */
	public static long convertToLong(byte[] bytes) 
	{
		long number=0;
		for (byte b : bytes) 
			number = (number << 8) + (b & 0xFF);
	    return number;
	 }
	
	/**
	 * 
	 * 
	 * @param byteArray
	 * @throws IOException
	 * @return byte[]
	 */
	public static byte[] merge(byte... byteArray) throws IOException
	{
		return byteArray;
	}

	/**
	 * 
	 * 
	 * @param byteArray
	 * @throws IOException
	 * @return byte[]
	 */
	public static byte[] merge(byte[]... byteArray) throws IOException
	{
		ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
		for (byte[] bs : byteArray) 
			byteOutput.write(bs);
		byte[] mergedArray = byteOutput.toByteArray();
		byteOutput.close();
		return mergedArray;
	}
	
	/**
	 * 
	 * 
	 * @param data
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @return Object
	 */
	public static Object toObject(byte[] data) throws IOException, ClassNotFoundException
	{
		ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data));
		return objectInputStream.readObject();
	}
	
	public static String toObjectJSON(byte[] data)
	{
		return new String(data, StandardCharsets.UTF_8);
	}
}
