package com.coldfyre.syrup.Util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
	
	public void def(Object message, String color){
		 echo(colorConvert(color) + message);
	}
	
	public static void noTS(Object message, String color){
		 System.out.println(colorConvert(color) + message + "\u001B[0m");
	}
	
	public void debug(Object message, String color){
		 echo(colorConvert(color) + "[DEBUG] " + message);
	}
	 
	 public static void info(Object message, String color){
		 echo(colorConvert(color) + "[INFO] " + message);
	 }

	 public static void warn(Object message, String color){
		 echo(colorConvert(color) + "[WARN] " + message);

	 }

	 public static void error(Object message, String color){
		 echo(colorConvert(color) + "[ERROR] " + message);
	 }

	 public void fatal(Object message, String color){
		 echo(colorConvert(color) + "[FATAL] " + message);
	 }

	 private static void echo(String message) {
		 Date ts = new Date();
		 SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println("["+ date.format(ts) + "] "+ message + "\u001B[0m");
		
	 }

	 private static String colorConvert(String color) {
		 if (color.equals("LIGHT_RED")) { return "\u001B[1;31m"; }
		 if (color.equals("LIGHT_GREEN")) { return "\u001B[1;32m"; }
		 if (color.equals("LIGHT_YELLOW")) { return "\u001B[1;33m"; }
		 if (color.equals("LIGHT_CYAN")) { return "\u001B[1;36m"; }


		 return "";
	 }	    
}