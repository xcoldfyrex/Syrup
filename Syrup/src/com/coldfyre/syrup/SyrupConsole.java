package com.coldfyre.syrup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketAddress;

import com.coldfyre.syrup.Util.Log;

public class SyrupConsole implements Runnable {

	BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
	
	String userInput = null;


	public void run() {
		boolean a = true;
		while (a == true || a == false) {
			try {
				userInput = stdIn.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if ( userInput != null) {
				String[] userInputArgs = userInput.split(" ");
				if (userInputArgs[0].equalsIgnoreCase("HELP")) {
					Syrup.log.def("ColdFyre's Syrup IRCD", "LIGHT_CYAN");
					Syrup.log.def("Commands: debug on|off, help, clients, whois <user>, chan <channel>, channels, links, squit <server>, connect, stop", "LIGHT_CYAN");
				} 
				
				else if (userInputArgs[0].equalsIgnoreCase("DEBUG")) {
					if (userInputArgs[1].equalsIgnoreCase("ON")) {
						Syrup.debugMode = true;
					}
					else {
						Syrup.debugMode = false;
					}
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("SQUIT")) 
				{
					Syrup.closeConnectorSocket();
				} 
				else if (userInputArgs[0].equalsIgnoreCase("CONNECT")) 
				{
					
					Syrup.openConnectorSocket();					

				} 
				else if (userInputArgs[0].equalsIgnoreCase("LINKS")) {
					int i = 0;
					String sname,sver;
					SocketAddress saddy;
					
					while (i < Syrup.WaffleClients.size()) {
						sname = Syrup.WaffleClients.get(i).RemoteServerName;
						saddy = Syrup.WaffleClients.get(i).RemoteServerAddress;
						sver = Syrup.WaffleClients.get(i).RemoteServerVersion;
						System.out.println(sname + "\t" + saddy + "\t" + sver);
						i++;
					}
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("CHANNELS")) {
					if (Syrup.IRCChannels.size() != 0) {
						String name,modes,SID;
						Syrup.log.def("I have " + Syrup.IRCChannels.size() + " Channels formed:", "LIGHT_CYAN");
						Syrup.log.def("=======================================", "LIGHT_CYAN");
						for (String key : Syrup.IRCChannels.keySet()) {
							IRCChannel channel;
							channel = Syrup.IRCChannels.get(key);
							name = channel.getChannelName();
							modes = channel.getChannelModes();
							SID = channel.getSID();
							System.out.println(name + "\t"  + modes + "\t " + channel.getUserCount() + "\t" + SID);
						}
					}
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("CHAN")) {
					Syrup.log.def("Info for channel:", "LIGHT_CYAN");					
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("CLIENTS")) {
					if (Syrup.WaffleIRCClients.size() != 0) {
						String name,sid,host;
						Syrup.log.def("I have " + Syrup.WaffleIRCClients.size() + " WaffleIRC client(s) connected:", "LIGHT_CYAN");
						Syrup.log.def("=======================================", "LIGHT_CYAN");
						for (String key : Syrup.WaffleIRCClients.keySet()) {
							WaffleIRCClient person;
							person = Syrup.WaffleIRCClients.get(key);
							sid = person.SID;
							name = person.nick;
							host = person.host;
							System.out.println(name + "\t" + key + "\t " + sid + "\t " + host);
						}
					} 
					else {
						System.out.println("No WaffleIRC clients connected!");
					}
					synchronized(Syrup.csIRCClient) {
						Syrup.log.def("I have " + Syrup.IRCClient.size() + " IRC client(s) connected:", "LIGHT_CYAN");
						Syrup.log.def("=======================================","LIGHT_CYAN");
						int i = 0;
						String name,host,UID, modes;
						
						for (String key : Syrup.IRCClient.keySet()) {
							IRCUser person;
							person = Syrup.IRCClient.get(key);

							name = person.nick;
							host = person.realhost;
							modes = person.getServerModes();
							UID = person.UID;
							if (name.length() <= 7) {
								System.out.println(name + "\t\t\t" + "\t" + UID + "\t" + host + "\t" + modes);
							}
							else if (name.length() <= 14)  {
								System.out.println(name + "\t\t" + "\t" + UID + "\t" + host + "\t" + modes);
							}
							else {
								System.out.println(name + "\t" + "\t" + UID + "\t" + host + "\t" + modes);
							}
							i++;
						}
					}
					
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("STOP")) 
				{
					Log.info("Shutting down..(caught shutdown from user)", "LIGHT_RED");
					Syrup.closeConnectorSocket();
					System.exit(1);
				}
				else 
				{	
					Syrup.log.def("Unkown command, see /help", "LIGHT_CYAN");

				}
			}
		}
	}
}