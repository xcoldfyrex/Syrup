package com.coldfyre.syrup.Util;

import java.awt.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

public class Config {
	
	public static String SID;
	public static String serverName;
    public static String connectorHost; 
	public static int connectorPort;
	public static String pre;
	public static String linkPassword;
	public static int localPort;
	public static String sqlurl;
	public static String sqluser;
	public static String sqlpassword;
	private static String debugString;
	public static boolean silentBurst = false;
	private static String silentBurstString;
	public static ArrayList<String> debugParams = new ArrayList();
	
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
			Log.info("CONFIG: Read link password", "LIGHT_YELLOW");
			sqlpassword = configFile.getProperty("sqlpassword");
			Log.info("CONFIG: Read SQL password", "LIGHT_YELLOW");
			sqlurl = configFile.getProperty("sqlurl");
			Log.info("CONFIG: Read SQL URL: " + sqlurl, "LIGHT_YELLOW");
			sqluser = configFile.getProperty("sqluser");
			Log.info("CONFIG: Read SQL user: " + sqluser, "LIGHT_YELLOW");
			pre = ":" + SID + " ";
			debugString = configFile.getProperty("ignoredDebugParams");
			silentBurstString = configFile.getProperty("silentBurst");
			if (silentBurstString.equalsIgnoreCase("true")) silentBurst = true;
			Log.info("CONFIG: Should I hide BURST?: " + silentBurst, "LIGHT_YELLOW");
			String[] temp = debugString.split(" ");
			
			int i = 0;
			while (i < temp.length) {
				debugParams.add(temp[i]);
				i++;
			}
			Log.info("CONFIG: Allowed debug params: " + debugParams, "LIGHT_YELLOW");

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