package com.coldfyre.syrup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.coldfyre.syrup.WaffleIRCClient;
import com.coldfyre.syrup.Util.Config;
import com.coldfyre.syrup.Util.Log;
import com.coldfyre.syrup.Util.SQL;

public class WaffleClient implements Runnable {
	protected String RemoteServerID;
	protected String RemoteServerHash;
	protected int RemoteServerPort;
	protected  SocketAddress RemoteServerAddress;
	protected String RemoteServerHostname;
	protected String RemoteServerName;
	protected String RemoteServerVersion;
	protected String lobbyChannel;
	protected String consoleChannel;
	protected String botName;
	protected String botUID;
	protected String SID;
	protected long BurstTS; 
	public long LastPong = 0;
	protected long connectTS;
	protected boolean badLink = false;
	protected long lobbyChannelTS = System.currentTimeMillis() / 1000L;
	
	public List<String> userChannels = new ArrayList<String>();
	public HashMap<String, WaffleIRCClient> WaffleIRCClients = new HashMap<String, WaffleIRCClient>();

	
	public boolean capabSent = false;
	public boolean capabStarted = false;
	public boolean threadOK = true;

	public boolean burstSent = false;
	
	protected Socket waffleSocket;
    
	protected PrintWriter out;
	protected String waffleStream = null;

	protected Socket clientSocket = null;
    protected String serverText   = null;
    
    public WaffleClient(Socket clientSocket, String serverText) {
    	this.connectTS = System.currentTimeMillis() / 1000L;
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
		this.RemoteServerAddress = clientSocket.getRemoteSocketAddress();
		this.RemoteServerName = "UNKNOWN SERVER: " + String.valueOf(this.RemoteServerAddress);
		this.BurstTS = 0;
		this.LastPong = System.currentTimeMillis() / 1000L;

    }

    public void run() {
    	if (!clientSocket.isConnected()) CloseSocket("Lost peer");
        BufferedReader in = null;  
        try
        {                                
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            WriteSocket("CAPAB START 1202");
        	
            while(this.clientSocket.isConnected() && threadOK  == true) {
            	try {
            		String clientCommand = in.readLine();
            		if (clientCommand == null) {
            			CloseSocket("End of stream.");
            		}
            		else {
            			ParseLinkCommand(clientCommand);
            			if (Syrup.debugMode) {
            				Log.info(this.RemoteServerAddress +" [IN] " + clientCommand, "");
            			}
            		}
            	}
            	catch(IOException ioe) {
            		System.out.println(ioe);
            	}
                
                
            }
        }
        catch(Exception e)
        {
        	Log.error("CAUGHT EXCEPTION FOR " + this.RemoteServerAddress + ": "  + e + " SHOWING TRACE BELOW", "LIGHT_RED");
        	e.printStackTrace();
            threadOK = false;
            try {
				in.close();
				this.out.close();
			} catch (IOException e1) {
                threadOK = false;
			}
        }
        finally
        {
            try
            {                    
                in.close();
                this.out.close();
            }
            catch(IOException ioe)
            {
            	this.threadOK = false;
            }
        } 
    }
	
	public boolean ParseLinkCommand(String data) {
		if (badLink) return false;
		String[] split = data.split(" ");
		String command = "";
		split = data.split(" ");
		if (split[0].startsWith(":")){
			split[0] = split[0].substring(1);
			command = split[1];
		}
		if (burstSent) {
			if (split[0].equalsIgnoreCase("%bot%")) split[0] = this.botName;
		}
		
		if (data.startsWith("ERROR")){
			System.out.println("\u001B[1;31m[ERROR]"+ data +" \u001B[0m");
			badLink = true;
			return false;

		}
		if (command.startsWith("CAPAB START")) {
	    	System.out.println("\u001B[1;33m[INFO] Incoming link from: "+this.RemoteServerAddress + "\u001B[0m");
			capabStarted = true;
			return true;
		}
		
		if (command.startsWith("CAPAB END")) {
			capabSent = true;
			return true;
		}
		
		if (command.startsWith("SQUIT")) {
			badLink = true;
			CloseSocket("Requested by peer");
		}
		
		if (split[0].startsWith("SERVER")) {
			if (split.length < 6) {
				// bad SERVER, add handler
			}
			
			this.RemoteServerName = split[1];
			this.RemoteServerHash = split[2];
			SIDGen SID = new SIDGen();
			this.RemoteServerID = SID.generateSID();
			while (Syrup.WaffleClientsSID.containsKey(this.RemoteServerID)){
				RemoteServerID = SID.generateSID();
				Log.info("Trying next SID: " + this.RemoteServerID, "LIGHT_YELLOW");
			}
			Syrup.WaffleClientsSID.put(this.RemoteServerID, this.RemoteServerName);
			this.RemoteServerVersion = Format.join(split, " ", 5);
			String sql = SQL.getWaffleSettings(this.RemoteServerName);
			System.out.println("SQL: " + sql);
			if (sql.equals("")) {
				CloseSocket("ERROR: "+ RemoteServerName +" Unable to verify link! You should never see this!");
				this.badLink = true;
				return false;
			}
			String[] sqlparams =  sql.split(" ");
			
			this.lobbyChannel = sqlparams[4];
			if (Syrup.IRCChannels.containsKey(lobbyChannel)) {
				this.lobbyChannelTS = Syrup.IRCChannels.get(lobbyChannel).getChannelTS();
				if (Syrup.debugMode) {
					Log.info(this.RemoteServerName + ": Lobby TS: " + this.lobbyChannelTS, "LIGHT_YELLOW");
				}
			} else {
				if (Syrup.debugMode) {
					Log.info(this.RemoteServerName + ": Lobby TS not found, using current time", "LIGHT_YELLOW");
				}
			}
			this.consoleChannel = sqlparams[5];
			addChannel(sqlparams[4]);
			if (Syrup.WaffleClients.containsKey(this.RemoteServerName)) {
				WriteServices("LINK: Connection to "+RemoteServerName +" failed with error: Server "+RemoteServerName+" already exists!");
				WriteSocket("ERROR : "+ RemoteServerName +" already exists!");
				CloseSocket("ERROR: "+ RemoteServerName +" already exists!");
				badLink = true;
				return false;
			}
			else {
				if (sqlparams[2].equals(this.RemoteServerHash)) {
					WriteServices("LINK: Verified incoming server connection inbound from "+RemoteServerName +"("+ RemoteServerVersion+")");
					
					SendBurst();
					return true;
				}
				else {
					WriteServices("LINK: Link auth failed for "+RemoteServerName);
					WriteSocket("ERROR :Invalid link hash!");
					CloseSocket("ERROR: Invalid link hash!");

					badLink = true;
					return false;
					
				}
			}
		}	
		

		if (command.startsWith("BURST")) {
			if (split.length == 3) {
				this.BurstTS = Integer.parseInt(split[2]);
				burstSent = true;
				WriteSocket(Config.pre +"PING " + Config.SID + " "+ RemoteServerID);
				WriteConnectorSocket(":"+Config.serverName + " SERVER " + RemoteServerName + " * 0 " + RemoteServerID + " " + RemoteServerVersion);
				String UID = Syrup.uidgen.generateUID(RemoteServerID);
				String sql = SQL.getWaffleSettings(RemoteServerName);
				String[] sqlparams =  sql.split(" ");
				WriteConnectorSocket(":" + RemoteServerID + " UID " + UID + " " + System.currentTimeMillis() / 1000L + " " + sqlparams[0]  + "/mc " + sqlparams[3] + " " + sqlparams[3] + " " + sqlparams[0] + " " + sqlparams[1] + " " + System.currentTimeMillis() / 1000L + " +r :Waffle Bot");
				WriteConnectorSocket(":" + RemoteServerID + " FJOIN " + lobbyChannel + " " + lobbyChannelTS + " +nt :o," + UID);
				WaffleIRCClient waffleircclient = new WaffleIRCClient(sqlparams[0],RemoteServerName,false,RemoteServerID,System.currentTimeMillis() / 1000L);
				this.WaffleIRCClients.put(UID, waffleircclient);
				this.botName = sqlparams[0];
				this.botUID = UID;
				WriteSocket(Config.pre + "CONFIG BOTNAME=" + this.botName + " LOBBY=" + this.lobbyChannel + " CONSOLE=" + this.consoleChannel);
		    	Log.info("Incoming link completed: " + RemoteServerName + " " + this.RemoteServerAddress, "LIGHT_YELLOW");
				for (String key : Syrup.IRCClient.keySet()) {
					IRCUser person;
					person = Syrup.IRCClient.get(key);
		    		WriteSocket(Config.pre + "UID " + person.nick + " " + person.ident + " " + person.hostmask + " ");
		    	
		    	}
				String channame;	
				for (String key : Syrup.IRCChannels.keySet()) {
					IRCChannel channel;
					channel = Syrup.IRCChannels.get(key);
					if (channel != null) {
						channame = channel.getChannelName();
						WriteSocket(Config.pre + "FJOIN " + channame + channel.getMemberListByNick());
					}
				}
				WriteServices("LINK: Finished bursting to "+RemoteServerName);
				Syrup.WaffleClients.put(RemoteServerName, this);

				return true;
				
			}

		}
		
		if (command.startsWith("QUIT")) {
			String senderUID;
			senderUID = getUIDFromNick(split[0]);

			if (senderUID != null) {
				this.WaffleIRCClients.remove(senderUID);
				String reason;
				if (split.length > 2) {
					reason = Format.join(split, " ", 2);
					if (reason.startsWith(":")) reason = reason.substring(1);
				} 
				else {
					reason = "";
				}
				WriteConnectorSocket(":" + senderUID + " QUIT :" + reason);
			}
			Log.info("QUIT " + split[0] + " from " + this.RemoteServerID, "LIGHT_GREEN");
		}
		
		if (command.startsWith("FJOIN")){
			//at present, this only supports one nick
			if (split.length == 6) {
				String channel = split[2];
				if (channel.equals("%lobby%")) channel = this.lobbyChannel;
				String[] joinedUsersNick = split[5].split(",");
				if (joinedUsersNick.length >= 1) {
					String senderUID = getUIDFromNick(joinedUsersNick[1]);
					if (senderUID.equals("")) {
						Log.error(joinedUsersNick[1] + " tried to join channel on " + this.RemoteServerID +", but could not find UID", "LIGHT_RED");

					} 
					else {
						this.WaffleIRCClients.get(senderUID).addChannel(channel);
						WriteConnectorSocket(":" + this.RemoteServerID + " FJOIN " + channel + " " + this.lobbyChannelTS + " +nt :," + senderUID);
					}
						
					
				}
				else {
					badLink = true;
					WriteSocket("ERROR :PROTCOL ERROR: MALFORMED FJOIN");
					CloseSocket("PROTCOL ERROR: MALFORMED FJOIN");
				}
				
			}
		}
		
		if (command.startsWith("UID")) {
			if (split.length >= 6) {
				if ((getUIDFromNick(split[3])) == "") {
					WaffleIRCClient waffleircclient = new WaffleIRCClient(split[3],split[4],false,this.RemoteServerID,System.currentTimeMillis() / 1000L);
					String UID = Syrup.uidgen.generateUID(this.RemoteServerID);
					this.WaffleIRCClients.put(UID, waffleircclient);
					Log.info("JOIN " + UID + "->" + split[3]+ " from " + this.RemoteServerID, "LIGHT_GREEN");
					WriteConnectorSocket(":" + this.RemoteServerID + " UID " + UID + " " + System.currentTimeMillis() / 1000L + " " + split[3]  + "/mc " + split[4] + " " + waffleircclient.hostmask + " " + split[3] + " " + split[5] + " " + System.currentTimeMillis() / 1000L + " +r :Dot");
				}
				else {
					Log.warn(split[3] + " tried to join twice from " + this.RemoteServerID, "LIGHT_YELLOW");
				}
			}
		}
		
		if (command.startsWith("PRIVMSG")) {
			//is a valid PRIVMSG?
			if (split.length < 4) return false;
			if (split[3].startsWith(":")) split[3] = split[3].substring(1);
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			String sourceUID = split[0];
			sourceUID = getUIDFromNick(sourceUID);
			String message = Format.join(split, " ", 3);
			String target = split[2];
			// is the target a channel or real irc client?
			if (! (Syrup.IRCChannels.containsKey(target) || target.equals("") || (Syrup.IRCClient.get(target) == null))) return false;
			if (Syrup.IRCClient.containsValue(target)) target = Syrup.IRCClient.get(target).UID;
			//make sure the client is really on this link
			if (split[0].equals("000")) {
				WriteConnectorSocket(":" + this.botUID + " PRIVMSG " + target + " :" + message);
			}
			else if (split[0].equalsIgnoreCase(this.botName)) {
				WriteConnectorSocket(":" + this.botUID + " PRIVMSG " + target + " :" + message);
			} 		
			else if (sourceUID == null) {
				//dunno how this would happen
			}
			else if (sourceUID.startsWith(this.RemoteServerID)) {
				WriteConnectorSocket(":" + sourceUID + " PRIVMSG " + target + " :" + message);
			} 
			
			
		}
		
		return false;
	}
    
	private String getUIDFromNick(String nick) {
		for (String key : this.WaffleIRCClients.keySet()) {
			if (WaffleIRCClients.get(key).nick.equalsIgnoreCase(nick)) return key;
			
		}
		return "";
	}
	public void WriteServices(String data) {
		WriteConnectorSocket(":" + Config.serverName+ " PRIVMSG #services :" + data);
	}
	
	public void CloseSocket(String reason) {
		if (Syrup.WaffleClientsSID.containsKey(this.RemoteServerID)) {
			Syrup.WaffleClientsSID.remove(this.RemoteServerID);
		}
    	Log.info("Start socket shutdown: " + this.RemoteServerAddress, "LIGHT_YELLOW" );
    	int lost = this.WaffleIRCClients.size();
    	if (this.WaffleIRCClients.size() != 0 ) {
    		for (String key : this.WaffleIRCClients.keySet()) {
				Log.warn(key + " lost from split: " + this.RemoteServerID, "LIGHT_YELLOW");
			} 
    	}
		this.WaffleIRCClients.clear();
		if (clientSocket.isConnected()) {
			Syrup.WaffleClients.remove(RemoteServerName);
	    	Log.info("Lost client link: " + RemoteServerName+ " "+this.RemoteServerAddress + " " + reason + " (lost " + lost + " clients)", "LIGHT_YELLOW" );
			WriteSocket(Config.pre +reason);
			WriteConnectorSocket(":" + Config.serverName+ " SQUIT " + RemoteServerName + " :" +reason);
			WriteServices("LINK: Server "+RemoteServerName +" split: " +reason);

			threadOK = false;
			try {
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	public void WriteConnectorSocket(String data) {
		if (Syrup.connected) {
			if (Syrup.debugMode) {
				Log.info(this.RemoteServerAddress +" [OUT(IRC)] " + data,"");
			}
    		Syrup.out.println(data);
    		Syrup.out.flush();
		}

    }
	
	public void WriteSocket(String data) {
		if (clientSocket.isConnected()) {
			if (Syrup.debugMode) {
				Log.info(this.RemoteServerAddress +" [OUT(CLIENT)] " + data,"");
			}
    		this.out.println(data);
			this.out.flush();
		}

    } 
	
	public boolean SendBurst() {
		
		WriteSocket(":1SY ENDBURST");
		return true;
	}
	
	public String getServerID() {
		return this.RemoteServerID;
	}
	
	public void addChannel(String channel) {
		if (!userChannels.contains(channel)) {
			this.userChannels.add(channel);
		}
	}
	
	public void removeChannel(String channel) {
		if (userChannels.contains(channel)) {
			this.userChannels.remove(channel);
		}	
	}
	
}