package com.coldfyre.syrup;

import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.io.*;

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
	public static String pre = ":1SY ";
	
	//console thread
	static SyrupConsole syrupConsole = null; 
	private static Thread consoleThread = null;
	
	//minecraft server listener thread
	static WaffleListener waffleListener = null; 
	private static Thread waffleListenerThread = null;
	

	
	public static List<IRCUser> IRCClient = new LinkedList<IRCUser>();

    public static void main(String[] args) throws IOException {

    	syrupConsole = new SyrupConsole();
		consoleThread = new Thread(syrupConsole);
		consoleThread.start();
		consoleThread.setName("Console Thread");
		System.out.println("[INFO] Started console thread");
		
		waffleListener = new WaffleListener(6667);
    	waffleListenerThread = new Thread(waffleListener);
    	waffleListenerThread.start();
    	waffleListenerThread.setName("Waffle Listener Thread");
		System.out.println("[INFO] Started client listener thread");

    	System.out.println("********** Starting ColdFyre's Syrup IRCD **********");
    	
        if (openConnectorSocket()) {
        	connected = true;
        }
        
        while (running) {
        	while (connected && connectorSocket != null) {
        		String connectorStream = in.readLine();
        		
        		if (connectorStream == null) {
        			System.out.println("[WARN] Lost link to " + connectorHost);
        			closeConnectorSocket();
        		} else {
        			System.out.println("->" + connectorStream);
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
    		WriteSocket("SERVER syrup.paradoxirc.net mUPhegEy9f+fu*acre_= 0 1SY :Syrup");
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
    	System.out.println("<-" + data);
    	out.println(data);

    }
    
    public static boolean openConnectorSocket() {
    	if (connected) {
        	System.out.println("[WARNING] Somehow tried to open socket twice?");
        	return true;
    	}
    	System.out.println("[INFO] Connecting to peer");

        try {
        	connectorSocket = new Socket(connectorHost, connectorPort);
            out = new PrintStream(connectorSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(connectorSocket.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("[ERROR] DNS Failure");
            return false;
        } catch (IOException e) {
            System.err.println("[ERROR] Can't connect to: " + connectorHost + " Reason:" +e);
            return false;
        }
    	System.out.println("[OK] Connected to peer");
    	
    	return true;
    }
    
    public static boolean closeConnectorSocket() {
		System.out.println("[INFO] Shutdown connector socket");
    	connected = false;
    	sentBurst = false;
    	sentCapab = false;
    	if ((connectorSocket != null) && connectorSocket.isConnected()) {
    		try { connectorSocket.close(); } catch (IOException e) { 
			}
    	}
    	return true;
    }
    
	
}