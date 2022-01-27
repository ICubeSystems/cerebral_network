package com.ics.synapse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.BitSet;

import com.ics.nceph.core.event.Event;
import com.ics.nceph.core.event.exception.ImproperEventBufferInstantiationException;
import com.ics.nceph.core.reactor.exception.ImproperReactorClusterInstantiationException;
import com.ics.nceph.core.reactor.exception.ReactorNotAvailableException;
import com.ics.util.ByteUtil;

public class MessageTest 
{
	
	public static void main(String[] args) throws IOException, ImproperReactorClusterInstantiationException, ReactorNotAvailableException, ImproperEventBufferInstantiationException, InterruptedException 
	{
		byte[] byteArray = "hi there".getBytes();
		
		//System.out.println(byteArray.length);
		ByteBuffer buffer = ByteBuffer.allocate(32);
		buffer.put(byteArray);
		//Event event = new Event.Builder().eventId(1000).name("test event").build();
		
		//ByteArrayOutputStream baos = new ByteArrayOutputStream();
		//ObjectOutputStream oos = new ObjectOutputStream(baos);
		//oos.writeObject(event);
		//oos.flush();
		//byte[] data = baos.toByteArray();
		//System.out.println(data);
		//oos.close();
		//baos.close();
		
		//System.out.println(print(byteArray));
		//System.out.println(print(data));

		
		/*ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte genesis = (byte)7;
		output.write(genesis);
		output.write(byteArray);
		output.write(data);
		System.out.println(genesis);
		byte[] gb = new byte[1];
		gb[0] = genesis;
		
		int number = 0;
		number = (number << 8) + (genesis & 0xFF);
		System.out.println(number);
		
		byte[] out = output.toByteArray();
		//System.out.println(print(out));*/
		
		
		//System.out.println(out.length);
		
		
		//EventBuffer eBuffer = new EventBuffer.Builder().event(event).build();
		//System.out.println(buffer.capacity() + ":" + buffer.position());
		
		byte b1 = (byte) 0x01;
		String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
		//System.out.println(s1); // 10000001
		
		BitSet flags = bitset(b1);
		//System.out.println(flags.get(0));
		flags.set(2);
		
		byte[] bb = flags.toByteArray();
		String s2 = String.format("%8s", Integer.toBinaryString(bb[0] & 0xFF)).replace(' ', '0');
		//System.out.println(s2); // 10000001
		//System.out.println(bb.length);
		
		//System.out.println(print(dataSize(1000)));
		byte[] bbb = dataSize(1000);
		for (byte b : bbb) {
			String sss = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
			//System.out.println(sss);
		}
		////System.out.println(print(ByteUtil.convertToByteArray(1000, 4))); 
		
		//System.out.println("--------------------------------------");
		long l = 2147483649L;
		byte[] bbbb = ByteUtil.convertToByteArray(4294967295L, 4);
		long value = 0;
		for (byte b : bbbb) {
			value = (value << 8) + (b & 0xFF);
			//System.out.println(value);
			//String sss = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
			//System.out.println(sss);
		}
		
		//System.out.println("final value:"+ByteBuffer.wrap(bbbb).getInt());
		//System.out.println("final value:"+fromByteArray(bbbb));
		
		byte[] temp = new byte[1];
		System.out.println(temp.length);
		
		
		byte b = (byte)161;
		temp = null;
		if(0x03==3)
			System.out.println("++++++++++++++++");
		
		System.out.print("Anurag - transferring....");
		Thread.sleep(2000);
		System.out.print("\rAnurag - transferred....  ");
		Thread.sleep(2000);
	}
	
	public static int convertToInt(byte b) 
	{
		//if (bytes.length > 4)
		//	throw InvalidConversionException 
	    return (0 << 8) + (b & 0xFF);
	 }
	
	public static long fromByteArray(byte[] bytes) 
	{
		long number=0;
		for (byte b : bytes) 
			number = (number << 8) + (b & 0xFF);
	    return number;
	 }
	
	public static byte[] dataSize(int i)
	{
		byte[] result = new byte[4];

		result[0] = (byte) (i >> 24);
		result[1] = (byte) (i >> 16);
		result[2] = (byte) (i >> 8);
		result[3] = (byte) (i);

		return result;
	}
	
	public static String print(byte[] bytes) 
	{
	    StringBuilder sb = new StringBuilder();
	    sb.append("[ ");
	    for (byte b : bytes) {
	        sb.append(String.format("0x%02X ", b));
	    }
	    sb.append("]");
	    return sb.toString();
	}
	
	public static BitSet bitset(byte b)
	{
		BitSet bitset = new BitSet(8);
		for (int i=0; i<8; i++) 
		{
		    if ((b & (1 << i)) > 0)
		    {
		        bitset.set(i);
		    }
		}
		return bitset;
	}
}
