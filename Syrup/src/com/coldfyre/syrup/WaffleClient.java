package com.coldfyre.syrup;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;

public class WaffleClient implements Runnable {
	public String RemoteServerID;
	public String RemoteServerHash;
	public int RemoteServerPort;
	public String RemoteServerAddress;
	public String RemoteServerHostname;
	public String RemoteServerName;
	public String RemoveServerVersion;
	
	public static Socket waffleSocket;
	public String line,input;
    
	public static PrintStream out;
    public static BufferedReader in = null;
	public static String waffleStream = null;


	public void run() {
		System.out.println("[INFO] Starting thread for " + waffleSocket.getRemoteSocketAddress());

	     //while (waffleSocket != null) {
				try {
					waffleStream = in.readLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

	        	if (waffleStream != null) {
	        		out.println("I got:" + waffleStream);
	        	}
	        //}

	}
	
	WaffleClient(Socket server) {
		waffleSocket = server;
		RemoteServerID = "ZZZ";
		RemoteServerHash = "";
	}
	
	public String getServerID() {
		return RemoteServerID;
	}
}