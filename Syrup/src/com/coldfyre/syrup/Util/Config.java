package com.coldfyre.syrup.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
	
	public static String SID;
	public static String serverName;
    public static String connectorHost; 
	public static int connectorPort;
	public static String pre;
	public static String linkPassword;
	public static int localPort;
	
	public static boolean GetProperties(){
		
		final Properties configFile = new Properties();

        try {
			configFile.load(new FileInputStream("syrup.properties"));
			try {
			serverName = configFile.getProperty("serverName");
			Log.info("CONFIG: Using server name: " + serverName, "LIGHT_YELLOW");
			SID = configFile.getProperty("SID");
			Log.info("CONFIG: Using server ID: " + SID, "LIGHT_YELLOW");
			connectorHost = configFile.getProperty("connectorHost");    
			Log.info("CONFIG: Using link target: " + connectorHost, "LIGHT_YELLOW");
			connectorPort = Integer.parseInt(configFile.getProperty("connectorPort"));
			Log.info("CONFIG: Using remote port: " + connectorPort, "LIGHT_YELLOW");
			localPort = Integer.parseInt(configFile.getProperty("localPort"));
			Log.info("CONFIG: Using local port: " + localPort, "LIGHT_YELLOW");
			linkPassword = configFile.getProperty("linkPassword");
			Log.info("CONFIG: Read password", "LIGHT_YELLOW");
			pre = ":" + SID + " ";
			Log.info("Config loaded", "LIGHT_GREEN");
			return true;
			} catch (NullPointerException ee) {
				Log.error("Invalid config, caught exception, please check.", "LIGHT_RED");
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			Log.error("Failed reading config", "LIGHT_RED");
			return false;

		}
	
	}
	
	public static void LoadProperties() {
		GetProperties();
	}
	
}