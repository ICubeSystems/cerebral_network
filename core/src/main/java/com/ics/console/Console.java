package com.ics.console;

public class Console 
{
	public static void info(String output)
	{
		System.out.println(output);
	}
	
	public static void error(String output)
	{
		System.out.println(ConsoleColors.RED + output + ConsoleColors.RESET);
	}
	
	public static void success(String output)
	{
		System.out.println(ConsoleColors.GREEN_BOLD_BRIGHT + output + ConsoleColors.RESET);
	}
}
