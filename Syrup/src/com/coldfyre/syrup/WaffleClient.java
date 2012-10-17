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
	public boolean burstGot;
	
	protected Socket waffleSocket;
    
	protected PrintWriter out;
	protected String waffleStream = null;

	protected Socket clientSocket = null;
    protected String serverText   = null;
    
    public WaffleClient(Socket clientSocket, String serverText) {
    	this.connectTS = System.currentTimeMillis() / 1000L;
        this.clientSocket = clientSocket;
        this.serverText = serverText;
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
            		threadOK = false;
            		Log.error(this.RemoteServerAddress +" Exception: " +ioe, "LIGHT_RED");
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
		if (data.startsWith("CAPAB START")) {
	    	System.out.println("\u001B[1;33m[INFO] Incoming link from: "+this.RemoteServerAddress + "\u001B[0m");
			capabStarted = true;
			return true;
		}
		
		if (data.startsWith("CAPAB END")) {
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
			if (!this.capabSent) {
				Log.info(RemoteServerName + " Trying to auth but never sent CAPAB!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Tried to send SERVER, but never sent CAPAB");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
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
			WriteConnectorSocket(":" + Config.serverName+ " SQUIT " + RemoteServerName + " :Flushing server prior to link");
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
			if (this.burstSent) {
		    	Log.info(RemoteServerName + " Trying to burst twice!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Tried to send BURST, but already BURST");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
			}
			if (!this.capabSent) {
				Log.info(RemoteServerName + " Trying to burst but never sent CAPAB!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Tried to send BURST, but never sent CAPAB");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
			}
			
			if (split.length == 3) {
				this.BurstTS = Integer.parseInt(split[2]);
				WriteSocket(Config.pre +"PING " + Config.SID + " "+ RemoteServerID);
				WriteConnectorSocket(":"+Config.serverName + " SERVER " + RemoteServerName + " * 0 " + RemoteServerID + " " + RemoteServerVersion);
				String UID = Syrup.uidgen.generateUID(RemoteServerID);
				String sql = SQL.getWaffleSettings(RemoteServerName);
				String[] sqlparams =  sql.split(" ");
				WriteConnectorSocket(":" + RemoteServerID + " UID " + UID + " " + System.currentTimeMillis() / 1000L + " " + sqlparams[0]  + "/mc " + sqlparams[3] + " " + sqlparams[3] + " " + sqlparams[0] + " " + sqlparams[1] + " " + System.currentTimeMillis() / 1000L + " +r :Waffle Bot");
				WriteConnectorSocket(":" + RemoteServerID + " FJOIN " + lobbyChannel + " " + lobbyChannelTS + " +nt :o," + UID);
				WriteConnectorSocket(":" + RemoteServerID + " FJOIN " + consoleChannel + " " + lobbyChannelTS + " +nt :o," + UID);
				Syrup.IRCChannels.get(lobbyChannel).addUser(RemoteServerID, "o");
				Syrup.IRCChannels.get(consoleChannel).addUser(RemoteServerID, "o");
				WaffleIRCClient waffleircclient = new WaffleIRCClient(sqlparams[0],RemoteServerName,false,RemoteServerID,System.currentTimeMillis() / 1000L);
				this.WaffleIRCClients.put(UID, waffleircclient);
				this.botName = sqlparams[0];
				this.botUID = UID;
				//send IRC clients back to WaffleClient
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
				WriteSocket(Config.pre + "ENDBURST");
				burstSent = true;
				return true;
				
			}

		}
		
		if (command.startsWith("QUIT")) {
			if (!this.burstSent) {
		    	Log.info(RemoteServerName + " Sent QUIT but never BURST!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Sent QUIT but never BURST");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
			}
			
			String senderUID;
			senderUID = getUIDFromNick(split[0]);
			if (!this.WaffleIRCClients.containsKey(senderUID)) return false;


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
			if (!this.burstSent) {
		    	Log.info(RemoteServerName + " Sent FJOIN but never BURST!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Sent FJOIN but never BURST");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
			}
			
			//at present, this only supports one nick
			if (split.length == 6) {
				String channel = split[2];
				String mode = "";
				String[] joinedUsersNick = split[5].split(",");
				if (channel.equals("%lobby%")) {
					channel = this.lobbyChannel;
					mode = joinedUsersNick[0];
				}
				else {
					if (channel.startsWith("#")){
						channel = channel + "/mc";
					}
					else {
						channel = "#" + channel + "/mc";
					}
				}
				if (joinedUsersNick.length >= 1) {
					String senderUID = getUIDFromNick(joinedUsersNick[1]);
					if (senderUID.equals("")) {
						Log.warn(joinedUsersNick[1] + " tried to join channel on " + this.RemoteServerID +", but could not find UID", "LIGHT_YELLOW");
						WriteServices("WARN: "+RemoteServerName + " " + joinedUsersNick[1] + " tried to join channel on " + this.RemoteServerID +", but could not find UID");
					} 
					else {
						this.WaffleIRCClients.get(senderUID).addChannel(channel);
						if (Syrup.IRCChannels.get(channel) == null) {
							IRCChannel newChannel = new IRCChannel(channel, System.currentTimeMillis() / 1000L, Config.SID);
							Syrup.IRCChannels.put(channel, newChannel);
						}
						Syrup.IRCChannels.get(channel).addUser(joinedUsersNick[1], "");
						WriteConnectorSocket(":" + this.RemoteServerID + " FJOIN " + channel + " " + this.lobbyChannelTS + " +nt " + mode + "," + senderUID);
						Syrup.WriteWaffleSockets(Config.pre + "FJOIN " + channel + " ," + joinedUsersNick[1] + "/mc",this.RemoteServerID);

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
			if (!this.burstSent) {
		    	Log.info(RemoteServerName + " Sent UID but never BURST!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Sent UID but never BURST");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
			}
			
			if (split.length == 5) {
				if ((getUIDFromNick(split[2])) == "") {
					//check protocol violations
					//if ())
					final long nowTS = System.currentTimeMillis() / 1000L;
					WaffleIRCClient waffleircclient = new WaffleIRCClient(split[2],split[3],false,this.RemoteServerID,System.currentTimeMillis() / 1000L);
					String UID = Syrup.uidgen.generateUID(this.RemoteServerID);
					this.WaffleIRCClients.put(UID, waffleircclient);
					Log.info("JOIN " + UID + "->" + split[2]+ "(" + split[4] + ") from " + this.RemoteServerID, "LIGHT_GREEN");
					WriteConnectorSocket(":" + this.RemoteServerID + " UID " + UID + " " + nowTS + " " + split[2]  + "/mc " + split[3] + " " + waffleircclient.hostmask + " " + split[2] + " " + split[4] + " " + System.currentTimeMillis() / 1000L + " +irc :WaffleIRC Client");
					//Tell other clients
					Syrup.WriteWaffleSockets(Config.pre + "UID " + split[3] + "/mc " + split[3] + " "+  split[4],null);
				}
				else {
					Log.warn(split[3] + " tried to join twice from " + this.RemoteServerID, "LIGHT_YELLOW");
					WriteServices("WARN: "+RemoteServerName + " split[3] +  tried to join twice from " + this.RemoteServerID);

				}
			}
			else {
				Log.warn("Malformed UID from " + this.RemoteServerID, "LIGHT_YELLOW");
				WriteServices("WARN: "+RemoteServerName + " Malformed UID from " + this.RemoteServerID);
			}
		}
		
		if (command.startsWith("PART")) {
			if (!this.burstSent) {
		    	Log.info(RemoteServerName + " Sent PART but never BURST!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Sent PART but never BURST");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
			}
			
			if (split.length < 3) return false;
			String channel = split[2];
			if (channel.startsWith("#")){
				channel = channel + "/mc";
			}
			else {
				channel = "#" + channel + "/mc";
			}
			String sourceUID = split[3];
			sourceUID = getUIDFromNick(sourceUID);
			if (!this.WaffleIRCClients.containsKey(sourceUID)) return false;
			if (sourceUID == null) return false;
			WriteConnectorSocket(":" + sourceUID + " PART " + channel);			
			
		}
		
		if (command.startsWith("PRIVMSG")) {
			if (!this.burstSent) {
		    	Log.info(RemoteServerName + " Sent PRIVMSG but never BURST!", "LIGHT_RED");
				WriteServices("ERROR: "+RemoteServerName + " Sent PRIVMSG but never BURST");
				CloseSocket("PROTOCOL ERROR");
		    	return false;
			}
			
			//is a valid PRIVMSG?
			if (split.length < 4) return false;
			if (split[3].startsWith(":")) split[3] = split[3].substring(1);
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			if (split[0].startsWith(":")) split[0] = split[0].substring(1);
			String sourceUID = split[0];
			sourceUID = getUIDFromNick(sourceUID);
			if (!this.WaffleIRCClients.containsKey(sourceUID) && !split[0].equals("000")) {
				Log.warn("Got PRIVMSG from client not on server " + split + " " + this.RemoteServerID, "LIGHT_YELLOW");
				return false;
			}
			String message = Format.join(split, " ", 3);
			String target = split[2];
			//blank msgs are a protocol violation. bad!
			if (message.replace(" ", "").equals("") || target.replace(" ", "").equals("") ) {
				Log.warn(split[0] + " sent privmsg with no target on " + this.RemoteServerID, "LIGHT_YELLOW");
				return false;
			}
			// is the target a channel or real irc client?
			if (! (Syrup.IRCChannels.containsKey(target) || 
					(Syrup.IRCChannels.containsKey("#" + target + "/mc")) || 
					target.equals("") ||
					target.equalsIgnoreCase(this.consoleChannel) ||
					(Syrup.IRCClient.get(target) == null))
					) 
					return false;
			
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
			//i think this is the lobby channel
			else if (sourceUID.startsWith(this.RemoteServerID) && (Syrup.IRCChannels.get(target) != null)) {
				WriteConnectorSocket(":" + sourceUID + " PRIVMSG " + target + " :" + message);
			} 
			else if (Syrup.IRCChannels.get("#" + target + "/mc") != null) {
				//this would occour if the target channel is not the lobby
				if (Syrup.IRCChannels.get("#" + target + "/mc").hasUID(split[0])) {
					Syrup.WriteWaffleSockets(":" + split[0] + "/mc PRIVMSG " + "#" + target + "/mc :" + message, this.RemoteServerID);
					WriteConnectorSocket(":" + sourceUID + " PRIVMSG " + "#" + target + "/mc :" + message);
				}
				
			}
			//lastly, this is a person to person message
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
		WriteSocket(":1SY BURST "  + System.currentTimeMillis() / 1000L);
		return true;
	}
	
	public String getServerID() {
		return this.RemoteServerID;
	}
	
	public String getUID(String nick) {
		for (String key : this.WaffleIRCClients.keySet()) {
			WaffleIRCClient client = WaffleIRCClients.get(key);
			if (client.nick.toLowerCase().equalsIgnoreCase(nick)) return key;
		}
		return null;
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