package com.coldfyre.syrup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
    
	public static PrintWriter out;
	public static String waffleStream = null;

	protected Socket clientSocket = null;
    protected String serverText   = null;
    

    public WaffleClient(Socket clientSocket, String serverText) {
        this.clientSocket = clientSocket;
        this.serverText   = serverText;
    }

    public void run() {
        BufferedReader in = null;  
        try
        {                                
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            out.println("CAPAB START 1202");
            out.flush();

            while(clientSocket.isConnected()) {
                String clientCommand = in.readLine();          
                System.out.println("Client Says :" + clientCommand);
                
                
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
                clientSocket.close();
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
        } 
    }
	
	public void ParseLinkCommand(String data) {
		
	}
    
	public String getServerID() {
		return RemoteServerID;
	}
}