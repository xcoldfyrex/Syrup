package com.coldfyre.syrup;

import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.io.*;

import com.coldfyre.syrup.UIDGen;

public class Syrup {

	public static PrintStream out;
    public static BufferedReader in = null;
    
    public static String connectorHost = "dev.mojeda.net"; 
	public static int connectorPort = 25402;
	public static Socket connectorSocket = null;	
	public static boolean running = true;
	public static boolean connected = false;
	public static boolean sentBurst = false;
	public static boolean sentCapab = false;
	public static boolean debugMode = true;
	public static String SID = "1SY";
	public static String pre = ":" + SID + " ";
	public static String serverName = "syrup.paradoxirc.net";
	
	//console thread
	static SyrupConsole syrupConsole = null; 
	private static Thread consoleThread = null;
	
	//minecraft server listener thread
	static WaffleListener waffleListener = null; 
	private static Thread waffleListenerThread = null;
	
	public static List<IRCUser> IRCClient = new LinkedList<IRCUser>();
	public static List<WaffleClient> WaffleClients = new LinkedList<WaffleClient>();
	public static HashMap<String, WaffleIRCClient> WaffleIRCClients = new HashMap<String, WaffleIRCClient>();
	
	public static UIDGen uidgen = new UIDGen();

    public static void main(String[] args) throws IOException {
    	System.out.println("\u001B[1;32m********** Starting ColdFyre's Syrup IRCD **********\u001B[0m");
    	syrupConsole = new SyrupConsole();
		consoleThread = new Thread(syrupConsole);
		consoleThread.start();
		consoleThread.setName("Console Thread");
		System.out.println("\u001B[1;33m[INFO] Started console thread\u001B[0m");
		
		waffleListener = new WaffleListener(6667);
    	waffleListenerThread = new Thread(waffleListener);
    	waffleListenerThread.start();
    	waffleListenerThread.setName("Waffle Listener Thread");
		System.out.println("\u001B[1;33m[INFO] Started client listener thread\u001B[0m");

    	
        if (openConnectorSocket()) {
        	connected = true;
        }
        
        while (running) {
        	while (connected && connectorSocket != null) {
        		String connectorStream = in.readLine();
        		
        		if (connectorStream == null) {
        			System.out.println("\u001B[1;33m[WARN] Lost link to " + connectorHost+"\u001B[0m");
        			closeConnectorSocket();
        		} else {
        			if (debugMode) {
        				System.out.println("->" + connectorStream);
        			}
        			ParseLinkCommand(connectorStream);
        		}
            
        		if (sentCapab && ! sentBurst){
        			SendBurst();
        		}       
        	}
        }
        
        
        
        //out.close();
        in.close();
        //stdIn.close();
        connectorSocket.close();
	}
    public static boolean ParseLinkCommand(String data) {
		String[] split = data.split(" ");
		String remoteSID = "";
		String command = "";
		split = data.split(" ");
		if (split[0].startsWith(":")){
			split[0] = split[0].substring(1);
			remoteSID = split[0];
			command = split[1];
		}
		if (data.startsWith("ERROR")) {
			System.out.println("\u001B[1;31m[ERROR] "+ data +" \u001B[0m");
			closeConnectorSocket();
		}
		//Got a PING
		if (command.equalsIgnoreCase("PING")) {
			WriteSocket(pre+"PONG 1SY "+ remoteSID);
		}
		
		if (command.equalsIgnoreCase("UID")) {
			//String UID=split[2];
			long idleTime = Long.parseLong(split[3]) * 1000;
			long signedOn = Long.parseLong(split[9]);
			if (split[11].startsWith(":")) split[11] = split[11].substring(1);
			String realname = Format.join(split, " ", 11);
			boolean isRegistered = split[10].contains("r");
			boolean isOper = split[10].contains("o");
			IRCUser ircuser = new IRCUser(split[4], realname, split[7], split[5], split[6], split[8], "", "", isRegistered, isOper, "", signedOn, idleTime);
			ircuser.isRegistered = isRegistered;
			//IRCDLink.uid2ircuser.put(UID, ircuser); // Add it to the hashmap
		}
		
    	if (data.equalsIgnoreCase("CAPAB START 1202")) {
    		WriteSocket("CAPAB START 1201");
    		WriteSocket("CAPAB CAPABILITIES :NICKMAX=33 CHANMAX=50 IDENTMAX=33 MAXTOPIC=500 MAXQUIT=500 MAXKICK=500 MAXGECOS=500 MAXAWAY=999 MAXMODES=1 HALFOP=1 PROTOCOL=1201");
    		WriteSocket("CAPAB END");  
    		WriteSocket("SERVER " + serverName + " mUPhegEy9f+fu*acre_= 0 " + SID +" :Syrup");
    		sentCapab = true;
    	}
    	
    	
    	return true;
    }
    
    public static boolean SendBurst() {
    	WriteSocket(pre+"BURST "+(System.currentTimeMillis() / 1000L));
		WriteSocket(pre+"VERSION : Syrup");
		//out.println(pre+"UID 1SYAAAAAA "+(System.currentTimeMillis() / 1000L)+" syrup.paradoxirc.net syrup.paradoxirc.net syrup.paradoxirc.net syrup.paradoxirc.net 127.0.0.1 "+(System.currentTimeMillis() / 1000L)+" +Siosw +ACKNOQcdfgklnoqtx : PONY");
		//out.println(":1SYAAAAAA OPERTYPE NetAdmin");
		WriteSocket(pre+"ENDBURST");
		sentBurst = true;
    	return true;
    }
    
    public static void WriteSocket(String data) {
		if (debugMode) {
			System.out.println("<-" + data);
		}
    	out.println(data);

    }
    
    public static boolean openConnectorSocket() {
    	if (connected) {
        	System.out.println("\u001B[1;31m[ERROR] Somehow tried to open connector socket twice?\u001B[0m");
        	return true;
    	}
    	System.out.println("\u001B[1;33m[INFO] Connecting to server: "+connectorHost+"\u001B[0m");
    	sentBurst = false;
    	sentCapab = false;
        try {
        	connectorSocket = new Socket(connectorHost, connectorPort);
            out = new PrintStream(connectorSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(connectorSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("\u001B[1;31m[ERROR] DNS Failure\u001B[0m");
            return false;
        } catch (IOException e) {
            System.err.println("\u001B[1;31m[ERROR] Can't connect to: " + connectorHost + " Reason:" +e +"\u001B[0m");
            return false;
        }
    	System.out.println("\u001B[1;32m[OK] Connected to server: "+connectorHost + "\u001B[0m");
    	
    	return true;
    }
    
    public static boolean closeConnectorSocket() {
		System.out.println("\u001B[1;33m[INFO] Shutdown connector socket\u001B[0m");
    	connected = false;
    	sentBurst = false;
    	sentCapab = false;
    	if ((connectorSocket != null) && connectorSocket.isConnected()) {
    		try { connectorSocket.close(); } catch (IOException e) { 
			}
    	}
    	return true;
    }
    
	public static int getWaffleClientServerName(String servername) {
			int i = 0;
			String sname;
			while (i < WaffleClients.size()) {
				sname = WaffleClients.get(i).RemoteServerName;
				System.out.println("LIST: " +i+ " " + sname + " " + servername);
				if (sname.equalsIgnoreCase(servername)) { return i; }
				else i++;
			}
			return -1;
	}
	
}