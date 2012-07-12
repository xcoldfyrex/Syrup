package com.coldfyre.syrup;

import java.net.*;
import java.util.HashMap;
import java.io.*;

import com.coldfyre.syrup.UIDGen;
import com.coldfyre.syrup.TS6.UID;
import com.coldfyre.syrup.Util.Config;
import com.coldfyre.syrup.Util.Log;

public class Syrup {

	public static PrintStream out;
    public static BufferedReader in = null;
    

	public static Socket connectorSocket = null;	
	public static boolean running = true;
	public static boolean connected = false;
	public static boolean sentBurst = false;
	public static boolean sentCapab = false;
	public static boolean debugMode = true;
	
	//classes
	public static UID UID = new UID();

	static class CriticalSection extends Object {
	}
	static public CriticalSection csIRCClient = new CriticalSection();

	
	//console thread
	static SyrupConsole syrupConsole = null; 
	private static Thread consoleThread = null;
	
	//pinger thread
	static WafflePING wafflePING = null; 
	private static Thread pingThread = null;
	
	//minecraft server listener thread
	static WaffleListener waffleListener = null; 
	private static Thread waffleListenerThread = null;
	
	public static HashMap<String, IRCUser> IRCClient = new HashMap<String, IRCUser>();
	public static HashMap<String, WaffleClient> WaffleClients = new HashMap<String, WaffleClient>();
	public static HashMap<String, WaffleIRCClient> WaffleIRCClients = new HashMap<String, WaffleIRCClient>();
	public static HashMap<String, IRCChannel> IRCChannels = new HashMap<String, IRCChannel>();
	public static HashMap<String, IRCServer> IRCServers = new HashMap<String, IRCServer>();
	
	public static UIDGen uidgen = new UIDGen();

	public static Log log = new Log();
    public static void main(String[] args) throws IOException {
    	

    	Log.info("###################### Starting ColdFyre's Syrup IRCD ######################", "LIGHT_GREEN");
		Log.info("Reading config..", "LIGHT_GREEN");
		
    	syrupConsole = new SyrupConsole();
		consoleThread = new Thread(syrupConsole);
		consoleThread.start();
		consoleThread.setName("Console Thread");
		Log.info("Started console thread", "LIGHT_GREEN");
		
    	wafflePING = new WafflePING();
    	pingThread = new Thread(wafflePING);
		pingThread.start();
		pingThread.setName("PING Thread");
		
    	if (Config.GetProperties()) {
    		if (openConnectorSocket()) {
        		connected = true;
        		out = new PrintStream(connectorSocket.getOutputStream(), true);
        		in = new BufferedReader(new InputStreamReader(connectorSocket.getInputStream()));
        	}
    		waffleListener = new WaffleListener(Config.localPort);
        	waffleListenerThread = new Thread(waffleListener);
        	waffleListenerThread.start();
        	waffleListenerThread.setName("Waffle Listener Thread");
        	Log.info("Started client listener thread", "LIGHT_GREEN");
    	}
    	
    	
    	
        while (running) {
        	while (connected && connectorSocket != null) {
        		String connectorStream = in.readLine();
        		if (connectorStream == null) {
        			log.warn("Lost link to " + Config.connectorHost, "LIGHT_RED");
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
        	    	Log.info("Connector socket lost connection" , "LIGHT_YELLOW");
        		}
        	}
        }
        
    	Log.info("Exiting main loop, terminating." , "LIGHT_YELLOW");
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
			Log.error(data, "LIGHT_RED");
			closeConnectorSocket();
		}
		//Got a PING
		if (command.equalsIgnoreCase("PING")) {
			String targetSID = split[3];
			if (targetSID.equalsIgnoreCase(Config.SID)) {
				WriteSocket(Config.pre+"PONG " + Config.SID + " " + remoteSID);
			} else 
			{
				WriteSocket(Config.pre+"PONG " + targetSID + " " + remoteSID);
			}
		}
		
		if (command.equalsIgnoreCase("FMODE")) {
			String mode;
			String modetarget = null;
			String finaltarget = "";
			mode = split[4];
			String source = split[0];
			//source
			if (IRCUser.getNick(source) != null) {
				source = IRCUser.getNick(source);
			}
			if (split.length > 6) {
				for (int i = 5; split.length>i; i++ ) {
					if (IRCClient.get(split[i]) != null) {
						
						modetarget = IRCClient.get(split[i]).nick;					
					} 
					else if (WaffleIRCClients.get(split[i]) != null) {
						modetarget = WaffleIRCClients.get(split[i]).nick + "/mc";
					}
					else {
						modetarget = split[i];
					}
					finaltarget = finaltarget + modetarget + " ";
				}
			}
			
			if (split.length > 5 ){
				WriteWaffleSockets(Config.pre + "FMODE " + split[2] + " " + source + " " + mode + " " + finaltarget);	
			}
		}
		
		if (command.equalsIgnoreCase("SERVER")) {
			IRCServer server;
			server = new IRCServer(split[2],split[0],"", split[5]);
			IRCServers.put(split[2], server);
	    	Log.info("Introduced server "+ split[2] , "LIGHT_YELLOW");
		}
		
		if (command.equalsIgnoreCase("SQUIT")) {
	    	Log.info("Lost server "+ split[2] + "from " + split[0] , "LIGHT_YELLOW");
	    	String SID = IRCServers.get(split[2]).SID;
	    	IRCServers.remove(split[2]);
	    	UID.purgeUIDByServer(SID);

		}
		
		if (command.equalsIgnoreCase("MODE")) {
			String target,newmode;
			//sender = split[0];
			target = split[2];
			if (split.length == 4) {
				newmode = split[3];
			} 
			else {
				newmode = split[3] + split [4];
			}
			IRCClient.get(target).setServerModes(newmode);
		}
		
		if (command.equalsIgnoreCase("FJOIN")) {
			String chanserv = split[0];
			String channame = split[2];
			long chanTS = Long.parseLong(split[3]);
			String chanmodes = split[4];
			int i = 0;
			while (i < 5) {
				split[i] = "";
				i++;
			}
					
			String[] people = data.split(" ");
			for (String user : people) {
				if (!user.contains(",")) continue;
				String infoz[] = user.split(",");
				//channel exists, just add people
				if (IRCChannels.get(channame) != null) {
					IRCChannels.get(channame).addUser(infoz[1], infoz[0]);
				}
				//is new chan
				else {
					IRCChannel channel = new IRCChannel(channame, chanTS, chanmodes, chanserv);
					IRCChannels.put(channame, channel);
					IRCChannels.get(channame).addUser(infoz[1], infoz[0]);
				}
			WriteWaffleSockets(Config.pre + "FJOIN " + channame + " ," + IRCClient.get(infoz[1]).nick);
			}
		}
		
		if (command.equalsIgnoreCase("UID")) {
			UID.add(split);
			WriteWaffleSockets(Config.pre + "UID " + split[4] + " " + split[6] + " "+  split[7]);
		}
		
		if (command.equalsIgnoreCase("PART")) {
			WriteWaffleSockets(":" + IRCClient.get(split[0]).nick + " PART " + split[2]);
			RemoveFromChannelsByUID(split[0]);
		}
		
		if (command.equalsIgnoreCase("QUIT")) {
			String reason;
			reason = Format.join(split, " ", 2);
			if (reason.startsWith(":")) reason = reason.substring(1);
			WriteWaffleSockets(":" + IRCClient.get(split[0]).nick + " QUIT " + reason);
			UID.removeUID(split[0]);
			RemoveFromChannelsByUID(split[0]);	
		}
		
		if (command.startsWith("NICK")) {
			WriteWaffleSockets(":" + IRCClient.get(split[0]).nick + " NICK " + split[2]);
			IRCClient.get(split[0]).nick = split[2];
		}
		
    	if (data.equalsIgnoreCase("CAPAB START 1202")) {
    		WriteSocket("CAPAB START 1201");
    		WriteSocket("CAPAB CAPABILITIES :NICKMAX=33 CHANMAX=50 IDENTMAX=33 MAXTOPIC=500 MAXQUIT=500 MAXKICK=500 MAXGECOS=500 MAXAWAY=999 MAXMODES=1 HALFOP=1 PROTOCOL=1201");
    		WriteSocket("CAPAB END");  
    		WriteSocket("SERVER " + Config.serverName + " " + Config.linkPassword + " 0 "+ Config.SID +" :Syrup");
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
    
    public static void RemoveFromChannelsByUID(String UID) {
    	for (String key : Syrup.IRCChannels.keySet()) {
			IRCChannel channel;
			channel = Syrup.IRCChannels.get(key);
			channel.removeUserByUID(UID);
		}
    }
    
    public static void RemoveFromChannelsBySID(String SID) {
    	for (String key : Syrup.IRCChannels.keySet()) {
			IRCChannel channel;
			channel = Syrup.IRCChannels.get(key);
			channel.removeUserBySID(SID);
		}
    }
    
    public static void WriteWaffleSockets(String data) {
    	for (String key : Syrup.WaffleClients.keySet()) {
			WaffleClient link;
			link = Syrup.WaffleClients.get(key);
			if (link != null) {
    			WaffleClients.get(key).WriteSocket(data);
			}
		}	
    }
    public static boolean SendBurst() {
    	WriteSocket(Config.pre+"BURST "+(System.currentTimeMillis() / 1000L));
		WriteSocket(Config.pre+"VERSION : Syrup");
		//out.println(pre+"UID 1SYAAAAAA "+(System.currentTimeMillis() / 1000L)+" syrup.paradoxirc.net syrup.paradoxirc.net syrup.paradoxirc.net syrup.paradoxirc.net 127.0.0.1 "+(System.currentTimeMillis() / 1000L)+" +Siosw +ACKNOQcdfgklnoqtx : PONY");
		//out.println(":1SYAAAAAA OPERTYPE NetAdmin");
		WriteSocket(Config.pre+"ENDBURST");
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
        	Log.error("Somehow tried to open connector socket twice?" , "LIGHT_RED");
        	return true;
    	}
    	if (connectorSocket != null) {
    			try { connectorSocket.close(); } catch (IOException e) { }
    			connectorSocket = null;
    	}
    	Log.info("Connecting to server: "+Config.connectorHost,"LIGHT_GREEN");
    	sentBurst = false;
    	sentCapab = false;
        try {
        	connectorSocket = new Socket(Config.connectorHost, Config.connectorPort);
        } catch (UnknownHostException e) {
        	Log.error("DNS Failure", "LIGHT_RED");
            return false;
        } catch (IOException e) {
        	Log.error("Can't connect to: " + Config.connectorHost + " Reason:" +e, "LIGHT_RED");
            return false;
        }
        if (connectorSocket == null) {
        	Log.error("Failed connect to: " + Config.connectorHost, "LIGHT_RED");
        	return false;
        }
        Log.info("Connected to server: "+Config.connectorHost, "LIGHT_GREEN");
        connected = true;
    	return true;
    }
    
    public static boolean closeConnectorSocket() {
    	Log.info("Shutdown connector socket", "LIGHT_YELLOW");
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