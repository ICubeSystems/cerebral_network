package com.ics.util;

/**
 * This is a utility class containing utility methods pertaining to operating system the JVM is running on
 * 
 * @author Anurag Arya
 * @since 13 Dec, 2021
 */
public class OSUtil 
{
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	/**
	 * This method returns true if the operating system the JVM is running on is Windows
	 * @return boolean - true if the OS is Windows, else false
	 */
	public static boolean isWindows() {
	    return (OS.indexOf("win") >= 0);
	}

	/**
	 * This method returns true if the operating system the JVM is running on is Macintosh
	 * @return boolean - true if the OS is Mac, else false
	 */
	public static boolean isMac() {
	    return (OS.indexOf("mac") >= 0);
	}

	/**
	 * This method returns true if the operating system the JVM is running on is Unix/ Linux
	 * @return boolean - true if the OS is Unix/ Linux, else false
	 */
	public static boolean isUnix() {
	    return (OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") > 0 );
	}

	/**
	 * This method returns true if the operating system the JVM is running on is Solaris
	 * @return boolean - true if the OS is Solaris, else false
	 */
	public static boolean isSolaris() {
	    return (OS.indexOf("sunos") >= 0);
	}
	
	/**
	 * This method returns the short name of the operating system the JVM is running on
	 * @return String
	 */
	public static String getOS(){
	    if (isWindows()) {
	        return "win";
	    } else if (isMac()) {
	        return "osx";
	    } else if (isUnix()) {
	        return "uni";
	    } else if (isSolaris()) {
	        return "sol";
	    } else {
	        return "err";
	    }
	}
	
}
