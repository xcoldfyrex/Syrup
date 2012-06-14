package com.coldfyre.syrup;

import java.net.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.io.*;

import com.coldfyre.syrup.UIDGen;
import com.coldfyre.syrup.TS6.UID;
import com.coldfyre.syrup.Util.Log;

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
	
	//classes
	public static UID UID = new UID();

	static class CriticalSection extends Object {
	}
	static public CriticalSection csIRCClient = new CriticalSection();

	
	//console thread
	static SyrupConsole syrupConsole = null; 
	private static Thread consoleThread = null;
	
	//minecraft server listener thread
	static WaffleListener waffleListener = null; 
	private static Thread waffleListenerThread = null;
	
	public static HashMap<String, IRCUser> IRCClient = new HashMap<String, IRCUser>();
	public static List<WaffleClient> WaffleClients = new LinkedList<WaffleClient>();
	public static HashMap<String, WaffleIRCClient> WaffleIRCClients = new HashMap<String, WaffleIRCClient>();
	public static HashMap<String, IRCChannel> IRCChannels = new HashMap<String, IRCChannel>();
	
	public static UIDGen uidgen = new UIDGen();

	public static Log log = new Log();
    public static void main(String[] args) throws IOException {
    	log.info("###################### Starting ColdFyre's Syrup IRCD ######################", "LIGHT_GREEN");
    	syrupConsole = new SyrupConsole();
		consoleThread = new Thread(syrupConsole);
		consoleThread.start();
		consoleThread.setName("Console Thread");
		log.info("Started console thread", "LIGHT_GREEN");
		
		waffleListener = new WaffleListener(6667);
    	waffleListenerThread = new Thread(waffleListener);
    	waffleListenerThread.start();
    	waffleListenerThread.setName("Waffle Listener Thread");
    	log.info("Started client listener thread", "LIGHT_GREEN");
    	
        if (openConnectorSocket()) {
        	connected = true;
        }
    	out = new PrintStream(connectorSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(connectorSocket.getInputStream()));
        
        while (running) {
        	while (connected && connectorSocket != null) {
        		String connectorStream = in.readLine();
        		
        		if (connectorStream == null) {
        			log.warn("Lost link to " + connectorHost, "LIGHT_RED");
        			closeConnectorSocket();
        		} else {
        			if (debugMode) {
        				log.def("[IN] " + connectorStream, "");
        			}
        			ParseLinkCommand(connectorStream);
        		}
            
        		if (sentCapab && ! sentBurst){
        			SendBurst();
        		} 
        		if (!connectorSocket.isConnected()) {
        	    	log.info("Connector socket lost connection" , "LIGHT_YELLOW");
        		}
        	}
        }
        
    	log.info("Exiting main loop, terminating." , "LIGHT_YELLOW");
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
			log.error(data, "LIGHT_RED");
			closeConnectorSocket();
		}
		//Got a PING
		if (command.equalsIgnoreCase("PING")) {
			WriteSocket(pre+"PONG 1SY "+ remoteSID);
		}
		
		if (command.equalsIgnoreCase("FJOIN")) {
			if (IRCChannels.get(split[2]) != null) {
				IRCChannels.get(split[2]).addUser(split[5], "r");
			}
			else {
				long TS = 0;//Long.getLong(split[3]);
				IRCChannel channel = new IRCChannel(split[2], TS, split[4]);
				IRCChannels.put(split[2], channel);
				IRCChannels.get(split[2]).addUser(split[5], "r");
			}
		}
		
		if (command.equalsIgnoreCase("UID")) {
			UID.add(split);
			WriteWaffleSockets(pre + "UID " + split[4] + " " + split[6] + " "+  split[7]);
		}
		
		if (command.equalsIgnoreCase("PART")) {
			WriteWaffleSockets(":" + IRCClient.get(split[0]).nick + " PART " + split[2]);
			//IRCChannels.get(split[2]).addUser(split[5], "r");

		}
		
		if (command.equalsIgnoreCase("QUIT")) {
			String reason;
			reason = Format.join(split, " ", 2);
			if (reason.startsWith(":")) reason = reason.substring(1);
			WriteWaffleSockets(":" + IRCClient.get(split[0]).nick + " QUIT " + reason);
			UID.removeUID(split[0]);
		}
		
		if (command.startsWith("NICK")) {
			WriteWaffleSockets(":" + IRCClient.get(split[0]).nick + " NICK " + split[2]);
			IRCClient.get(split[0]).nick = split[2];
		}
		
    	if (data.equalsIgnoreCase("CAPAB START 1202")) {
    		WriteSocket("CAPAB START 1201");
    		WriteSocket("CAPAB CAPABILITIES :NICKMAX=33 CHANMAX=50 IDENTMAX=33 MAXTOPIC=500 MAXQUIT=500 MAXKICK=500 MAXGECOS=500 MAXAWAY=999 MAXMODES=1 HALFOP=1 PROTOCOL=1201");
    		WriteSocket("CAPAB END");  
    		WriteSocket("SERVER " + serverName + " mUPhegEy9f+fu*acre_= 0 " + SID +" :Syrup");
    		sentCapab = true;
    	}
    	
		if (command.startsWith("PRIVMSG")) {
			if (split[3].startsWith(":")) split[3] = split[3].substring(1);
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if (split[0].startsWith(":")) split[0] = split[0].substring(1);

			String message = Format.join(split, " ", 3);
			String source = IRCUser.getNick(split[0]);
			String target = split[2];
			WriteWaffleSockets(":" + source + " PRIVMSG " + target + " :" + message);			
		}
		
		if (command.startsWith("IDLE")) {
			WriteSocket(":" + split[2] + " IDLE " + split[0] + " 0 0");
		}
    	
    	return true;
    }
    
    public static void WriteWaffleSockets(String data) {
    	int i = 0;
    	if (WaffleClients.size() != 0) {
    		while (i <  WaffleClients.size()) {
    			WaffleClients.get(i).WriteSocket(data);
    			i++;
    		}
		}     
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
			log.def("[OUT] " + data, "");
		}
    	out.println(data);

    }
    
    public static boolean openConnectorSocket() {
    	if (connected) {
        	log.error("Somehow tried to open connector socket twice?" , "LIGHT_RED");
        	return true;
    	}
    	if (connectorSocket != null) {
    			try { connectorSocket.close(); } catch (IOException e) { }
    			connectorSocket = null;
    	}
    	log.info("Connecting to server: "+connectorHost,"LIGHT_GREEN");
    	sentBurst = false;
    	sentCapab = false;
        try {
        	connectorSocket = new Socket(connectorHost, connectorPort);
        } catch (UnknownHostException e) {
        	log.error("DNS Failure", "LIGHT_RED");
            return false;
        } catch (IOException e) {
        	log.error("Can't connect to: " + connectorHost + " Reason:" +e, "LIGHT_RED");
            return false;
        }
        if (connectorSocket == null) {
        	log.error("Failed connect to: " + connectorHost, "LIGHT_RED");
        	return false;
        }
        log.info("Connected to server: "+connectorHost, "LIGHT_GREEN");
        connected = true;
    	return true;
    }
    
    public static boolean closeConnectorSocket() {
    	log.info("Shutdown connector socket", "LIGHT_YELLOW");
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