package com.ics.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * This is the information class containing information regarding the operating system on the machine the JVM is currently running on
 * 
 * @author Anurag Arya
 * @since 13 Dec, 2021
 */
public class OSInfo {

	/**
	 * Name of the operating system on the machine the JVM is currently running on
	 */
	public String name;
	
	
	/**
	 * Number of CPU cores available on the machine the JVM is currently running on
	 */
	public int numberOfCPUCores;
	
	/**
	 * default constructor
	 */
	public OSInfo()
	{
		numberOfCPUCores = getNumberOfCPUCores();
		System.out.println("++++++++++++");
	}
	
	/**
	 * This is a private method which runs a command process to query the OS and get the number of CPU cores available
	 * @return
	 */
	private int getNumberOfCPUCores() 
	{
	    String command = "";
	    if(OSUtil.isMac()){
	        command = "sysctl -n machdep.cpu.core_count";
	    }else if(OSUtil.isUnix()){
	        command = "lscpu";
	    }else if(OSUtil.isWindows()){
	        command = "cmd /C WMIC CPU Get /Format:List";
	    }
	    Process process = null;
	    int numberOfCores = 0;
	    int sockets = 0;
	    try {
	        if(OSUtil.isMac()){
	            String[] cmd = { "/bin/sh", "-c", command};
	            process = Runtime.getRuntime().exec(cmd);
	        }else{
	            process = Runtime.getRuntime().exec(command);
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    BufferedReader reader = new BufferedReader(
	            new InputStreamReader(process.getInputStream()));
	    String line;

	    try {
	        while ((line = reader.readLine()) != null) {
	            if(OSUtil.isMac()){
	                numberOfCores = line.length() > 0 ? Integer.parseInt(line) : 0;
	            }else if (OSUtil.isUnix()) {
	                if (line.contains("Core(s) per socket:")) {
	                    numberOfCores = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
	                }
	                if(line.contains("Socket(s):")){
	                    sockets = Integer.parseInt(line.split("\\s+")[line.split("\\s+").length - 1]);
	                }
	            } else if (OSUtil.isWindows()) {
	                if (line.contains("NumberOfCores")) {
	                    numberOfCores = Integer.parseInt(line.split("=")[1]);
	                }
	            }
	        }
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    if(OSUtil.isUnix()){
	        return numberOfCores * sockets;
	    }
	    return numberOfCores;
	}
}
