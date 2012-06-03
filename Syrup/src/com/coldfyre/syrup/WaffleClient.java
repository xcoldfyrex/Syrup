package com.coldfyre.syrup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketAddress;

import com.coldfyre.syrup.WaffleIRCClient;

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
		String remoteSID = "";
		String command = "";
		split = data.split(" ");
		if (split[0].startsWith(":")){
			split[0] = split[0].substring(1);
			remoteSID = split[0];
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
			this.RemoteServerID = split[4];
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
		    	System.out.println("\u001B[1;33m[INFO] Incoming link completed: " + RemoteServerName+ " "+this.RemoteServerAddress + "\u001B[0m");
				return true;
				
			}

		}
		
		if (command.startsWith("QUIT")) {
			WaffleIRCClient person;
			if ((person = Syrup.WaffleIRCClients.get(split[0])) != null) {
				Syrup.WaffleIRCClients.remove(person);
				String reason;
				if (split.length > 2) {
					reason = Format.join(split, " ", 2);
					if (reason.startsWith(":")) reason = reason.substring(1);
				} 
				else {
					reason = "";
				}
				WriteConnectorSocket(":" + split[0] + " QUIT :" + reason);
			}
			System.out.println("\u001B[1;33m[INFO] QUIT " + split[0] + " from " + RemoteServerID + "\u001B[0m");
		}
		
		if (command.startsWith("FJOIN")){
			String channel = split[2];
			WriteConnectorSocket(":" + RemoteServerID + " FJOIN " + channel + " " + System.currentTimeMillis() / 1000L + " +nt " + split[5]);
		}
		
		if (command.startsWith("UID")) {
			if (split.length >= 13) {
				WaffleIRCClient waffleircclient = new WaffleIRCClient(split[4],split[5],false,RemoteServerID,System.currentTimeMillis() / 1000L);
				Syrup.WaffleIRCClients.put(split[2], waffleircclient);
				String UID = Syrup.uidgen.generateUID(RemoteServerID);
				System.out.println("\u001B[1;33m[INFO] JOIN " + split[2] + " from " + RemoteServerID + "\u001B[0m");
				WriteConnectorSocket(":" + RemoteServerID + " UID " + UID + " " + System.currentTimeMillis() / 1000L + " " + split[4]  + " " + split[5] + " " + split[5] + " " + split[4] + " " + "127.0.0.0 " + System.currentTimeMillis() / 1000L + " +r : Dot");
			}
		}
		
		
		return false;
	}
    
	public void WriteServices(String data) {
		WriteConnectorSocket(":" + Syrup.serverName+ " PRIVMSG #services :" + data);
	}
	
	public void CloseSocket(String reason) {
		Syrup.WaffleIRCClients.remove(RemoteServerID+"*");
		
		if (clientSocket.isConnected()) {
			int waffleClient = Syrup.getWaffleClientServerName(RemoteServerName);
			if (waffleClient >= 0) {
				Syrup.WaffleClients.remove(waffleClient);
			}
	    	System.out.println("\u001B[1;33m[INFO] Lost client link: " + RemoteServerName+ " "+this.RemoteServerAddress + " " + reason + "\u001B[0m");
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