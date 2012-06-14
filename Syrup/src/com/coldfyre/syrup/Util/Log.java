package com.coldfyre.syrup.Util;

public class Log {
	
	public void def(Object message, String color){
		 echo(colorConvert(color) + message);
	}
	
	public void debug(Object message, String color){
		 echo(colorConvert(color) + "[DEBUG] " + message);
	}
	 
	 public void info(Object message, String color){
		 echo(colorConvert(color) + "[INFO] " + message);
	 }

	 public void warn(Object message, String color){
		 echo(colorConvert(color) + "[WARN] " + message);

	 }

	 public void error(Object message, String color){
		 echo(colorConvert(color) + "[ERROR] " + message);
	 }

	 public void fatal(Object message, String color){
		 echo(colorConvert(color) + "[FATAL] " + message);
	 }

	 private void echo(String message) {
		 java.util.Date time=new java.util.Date((long)System.currentTimeMillis());
		 System.out.println("["+ time + "] "+ message + "\u001B[0m");
	 }

	 private static String colorConvert(String color) {
		 if (color.equals("LIGHT_RED")) { return "\u001B[1;31m"; }
		 if (color.equals("LIGHT_GREEN")) { return "\u001B[1;32m"; }
		 if (color.equals("LIGHT_YELLOW")) { return "\u001B[1;33m"; }
		 if (color.equals("LIGHT_CYAN")) { return "\u001B[1;36m"; }


		 return "";
	 }	    
}