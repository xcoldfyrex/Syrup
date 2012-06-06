package com.coldfyre.syrup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

import com.coldfyre.syrup.WaffleIRCClient;
import com.coldfyre.syrup.TS6.UID;

public class WaffleClient implements Runnable {
	protected String RemoteServerID;
	protected String RemoteServerHash;
	protected int RemoteServerPort;
	protected  SocketAddress RemoteServerAddress;
	protected String RemoteServerHostname;
	protected String RemoteServerName;
	protected String RemoteServerVersion;
	protected int BurstTS; 
	protected int LastPong;
	
	public boolean capabSent = false;
	public boolean capabStarted = false;
	public boolean threadOK = true;

	public boolean burstSent = false;
	
	public static Socket waffleSocket;
    
	public static PrintWriter out;
	public static String waffleStream = null;

	protected Socket clientSocket = null;
    protected String serverText   = null;
    

    public WaffleClient(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
		this.RemoteServerAddress = clientSocket.getRemoteSocketAddress();
    }

    public void run() {
        BufferedReader in = null;  
        try
        {                                
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            WriteSocket("CAPAB START 1202");
            
            while(clientSocket.isConnected() && threadOK  == true) {
                String clientCommand = in.readLine();
                if (clientCommand == null) {
                    CloseSocket("End of stream.");
                }
                else {
                	ParseLinkCommand(clientCommand);
                	if (Syrup.debugMode) {
                		System.out.println(this.RemoteServerAddress +" ->" + clientCommand);
                	}
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {                    
                in.close();
                out.close();
                CloseSocket("Socket closed.");
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        } 
    }
	
	public boolean ParseLinkCommand(String data) {
		String[] split = data.split(" ");
		String command = "";
		split = data.split(" ");
		if (split[0].startsWith(":")){
			split[0] = split[0].substring(1);
			command = split[1];
		}
		
		if (data.startsWith("ERROR")){
			System.out.println("\u001B[1;31m[ERROR]"+ data +" \u001B[0m");

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
		
		if (split[0].startsWith("SERVER")) {
			if (split.length < 6) {
				// bad SERVER, add handler
			}
			
			this.RemoteServerName = split[1];
			this.RemoteServerHash = split[2];
			SIDGen SID = new SIDGen();
			RemoteServerID = SID.generateSID();
			this.RemoteServerVersion = Format.join(split, " ", 5);
			
			int waffleClient = Syrup.getWaffleClientServerName(RemoteServerName);
			System.out.println("ID " + waffleClient);
			if (waffleClient >= 0) {

				WriteServices("LINK: Connection to "+RemoteServerName +" failed with error: Server "+RemoteServerName+" already exists!");
				CloseSocket("ERROR: "+ RemoteServerName +" already exists!");
				return false;
			}
			else {
				WriteServices("LINK: Verified incoming server connection inbound from "+RemoteServerName +"("+ RemoteServerVersion+")");
				SendBurst();
				return true;
			}

		}	
		

		if (command.startsWith("BURST")) {
			if (split.length == 3) {
				this.BurstTS = Integer.parseInt(split[2]);
				burstSent = true;
				WriteServices("LINK: Finished bursting to "+RemoteServerName);
				WriteSocket(Syrup.pre +"PING " + Syrup.SID + " "+ RemoteServerID);
				WriteConnectorSocket(":"+Syrup.serverName + " SERVER " + RemoteServerName + " * 0 " + RemoteServerID + " " + RemoteServerVersion);
				Syrup.WaffleClients.add(this);
				//String UID = Syrup.uidgen.generateUID(RemoteServerID);
				//WriteConnectorSocket(":" + RemoteServerID + " UID " + UID + " " + UID  + " " + RemoteServerName + " " + RemoteServerName + " Andy Dick " + "127.0.0.0 " + System.currentTimeMillis() / 1000L + " +r : Dot");
		    	Syrup.log.info("Incoming link completed: " + RemoteServerName+ " "+this.RemoteServerAddress, "LIGHT_YELLOW");
				return true;
				
			}

		}
		
		if (command.startsWith("QUIT")) {
			String senderUID;
			senderUID = UID.GetWaffleClientUID(split[0]);

			if (senderUID != null) {
				Syrup.WaffleIRCClients.remove(senderUID);
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
			Syrup.log.info("QUIT " + split[0] + " from " + RemoteServerID, "LIGHT_GREEN");
		}
		
		if (command.startsWith("FJOIN")){
			//at present, this only supports one nick
			if (split.length == 6) {
				String channel = split[2];
				String[] joinedUsersNick = split[5].split(",");
				if (joinedUsersNick.length >= 1) {
					String senderUID,SID, tempnick;
					senderUID = UID.GetWaffleClientUID(joinedUsersNick[1]);
					SID = UID.GetWaffleClientSID(joinedUsersNick[1]);
					tempnick = UID.GetWaffleClientNick(joinedUsersNick[1]);
					if (tempnick.equalsIgnoreCase(joinedUsersNick[1]) && SID.equalsIgnoreCase(RemoteServerID)) { 
						WriteConnectorSocket(":" + RemoteServerID + " FJOIN " + channel + " " + System.currentTimeMillis() / 1000L + " +nt :," + senderUID);
					}
				}
				else {
					CloseSocket("PROTCOL ERROR: MALFORMED FJOIN");
				}
				
			}
		}
		
		if (command.startsWith("UID")) {
			if (split.length >= 11) {
				if ((UID.GetWaffleClientUID(split[3])) == null) {
					WaffleIRCClient waffleircclient = new WaffleIRCClient(split[3],split[4],false,RemoteServerID,System.currentTimeMillis() / 1000L);
					String UID = Syrup.uidgen.generateUID(RemoteServerID);
					Syrup.WaffleIRCClients.put(UID, waffleircclient);
					Syrup.log.info("JOIN " + UID + "->" + split[3]+ " from " + RemoteServerID, "LIGHT_GREEN");
					WriteConnectorSocket(":" + RemoteServerID + " UID " + UID + " " + System.currentTimeMillis() / 1000L + " " + split[3]  + "/mc " + split[3] + " " + split[5] + " " + split[3] + " " + "127.0.0.0 " + System.currentTimeMillis() / 1000L + " +r :Dot");
				}
			}
		}
		
		if (command.startsWith("PRIVMSG")) {
			if (split[3].startsWith(":")) split[3] = split[3].substring(1);
			if (split[2].startsWith(":")) split[2] = split[2].substring(1);
			String sourceUID = split[0];
			sourceUID = UID.GetWaffleClientUID(sourceUID);
			String message = Format.join(split, " ", 3);
			String target = split[2];
			WriteConnectorSocket(":" + sourceUID + " PRIVMSG " + target + " :" + message);
			
		}
		
		return false;
	}
    
	public void WriteServices(String data) {
		WriteConnectorSocket(":" + Syrup.serverName+ " PRIVMSG #services :" + data);
	}
	
	public void CloseSocket(String reason) {
		int i = 0;
		if (Syrup.WaffleIRCClients.size() != 0) {
			for (String key : Syrup.WaffleIRCClients.keySet()) {
				if (key.startsWith(RemoteServerID)) Syrup.WaffleIRCClients.remove(key);
				i++;
			}
		} 
		
		if (clientSocket.isConnected()) {
			int waffleClient = Syrup.getWaffleClientServerName(RemoteServerName);
			if (waffleClient >= 0) {
				Syrup.WaffleClients.remove(waffleClient);
			}
	    	Syrup.log.info("Lost client link: " + RemoteServerName+ " "+this.RemoteServerAddress + " " + reason + " (lost " + i + " clients)", "LIGHT_YELLOW" );
			WriteSocket(Syrup.pre +reason);
			WriteConnectorSocket(":" + Syrup.serverName+ " SQUIT " + RemoteServerName + " :" +reason);
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
		if (Syrup.debugMode) {
			System.out.println(this.RemoteServerAddress +" <-" + data);
		}
    	Syrup.out.println(data);
    	Syrup.out.flush();

    }
	
	public void WriteSocket(String data) {
		if (Syrup.debugMode) {
			System.out.println(this.RemoteServerAddress +" <-" + data);
		}
    	out.println(data);
		out.flush();

    } 
	
	public boolean SendBurst() {
		WriteSocket(":1SY ENDBURST");
		return true;
	}
	
	public String getServerID() {
		return RemoteServerID;
	}
	
}