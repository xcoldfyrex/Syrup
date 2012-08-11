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
					Log.noTS("ColdFyre's Syrup IRCD", "LIGHT_CYAN");
					Log.noTS("Commands: debug on|off, help, clients, whois <user>, chan <channel>, channels, links, squit <server>, connect, stop", "LIGHT_CYAN");
				} 
				
				else if (userInputArgs[0].equalsIgnoreCase("DEBUG")) {
					if (userInputArgs.length == 1) continue;
					if (userInputArgs[1].equalsIgnoreCase("ON")) {
						Syrup.debugMode = true;
						Log.noTS("Debug ON", "LIGHT_CYAN");
					}
					else {
						Log.noTS("Debug OFF", "LIGHT_CYAN");
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
					if (Syrup.WaffleClients.size() == 0) {
						Log.noTS("No Waffle links connected!", "LIGHT_CYAN");
						Log.noTS("=======================================", "LIGHT_CYAN");
					} 
					else {
						String sname,sver;
						SocketAddress saddy;
						Log.noTS(Syrup.WaffleClients.size() + "Waffle links connected", "LIGHT_CYAN");
						Log.noTS("=======================================", "LIGHT_CYAN");

						for (String key : Syrup.WaffleClients.keySet()) {
							WaffleClient link;
							link = Syrup.WaffleClients.get(key);
							if (link != null) {
								sname = link.RemoteServerName;
								saddy = link.RemoteServerAddress;
								sver = link.RemoteServerVersion;
								System.out.println(sname + "\t" + saddy + "\t" + sver + "\t" + (System.currentTimeMillis() / 1000L - link.LastPong));
							}
						}	
					}
					if (Syrup.IRCServers.size() != 0) {
						String sname,sver,parent;
						Log.noTS(Syrup.IRCServers.size() + " IRC server links", "LIGHT_CYAN");
						Log.noTS("=======================================", "LIGHT_CYAN");

						for (String key : Syrup.IRCServers.keySet()) {
							IRCServer link;
							link = Syrup.IRCServers.get(key);
							if (link != null) {
								sname = link.servername;
								parent = link.parent;
								sver = link.version;
								System.out.println(sname + "\t" + parent + "\t" + sver);
							}
						}
					}
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("CHANNELS")) {
					if (Syrup.IRCChannels.size() != 0) {
						String name,modes,SID;
						Log.noTS("I have " + Syrup.IRCChannels.size() + " Channels formed:", "LIGHT_CYAN");
						Log.noTS("=======================================", "LIGHT_CYAN");
						for (String key : Syrup.IRCChannels.keySet()) {
							IRCChannel channel;
							channel = Syrup.IRCChannels.get(key);
							if (channel != null) {
								name = channel.getChannelName();
								modes = channel.getChannelModes();
								SID = channel.getSID();
								System.out.println(name + "\t\t\t"  + modes + "\t " + channel.getUserCount() + "\t" + SID);
							}
						}
					}
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("CHAN")) {
					if (userInputArgs.length == 2) {
						Log.noTS("Info for channel:", "LIGHT_CYAN");
						Log.noTS("=======================================", "LIGHT_CYAN");
						IRCChannel channel;
						channel = Syrup.IRCChannels.get(userInputArgs[1]);
						System.out.println("TS: " + channel.getChannelTS()  + " Modes: " + channel.getChannelModes() + "\tUSers: "  + channel.getUserCount());	
						Log.noTS("=======================================", "LIGHT_CYAN");
						for (String chanmember : channel.Members.keySet()) {
							IRCUser person;
							person = Syrup.IRCClient.get(chanmember);
							System.out.println(person.nick + "\t" );
							
						}
					}
				}
				
				else if (userInputArgs[0].equalsIgnoreCase("CLIENTS")) {
					if (Syrup.WaffleClients.size() != 0) {
						//String name,sid,host;
						//Log.noTS("I have " + Syrup.WaffleIRCClients.size() + " WaffleIRC client(s) connected:", "LIGHT_CYAN");
						//Log.noTS("=======================================", "LIGHT_CYAN");
						//for (String key : Syrup.WaffleIRCClients.keySet()) {
						//	WaffleIRCClient person;
						//	person = Syrup.WaffleIRCClients.get(key);
						//	sid = person.SID;
						//	name = person.nick;
						//	host = person.host;
						//	System.out.println(name + "\t" + key + "\t " + sid + "\t " + host);
						//}
					} 
					else {
						Log.noTS("No WaffleIRC clients connected!", "LIGHT_CYAN");
						Log.noTS("=======================================", "LIGHT_CYAN");
					}
					synchronized(Syrup.csIRCClient) {
						Log.noTS("I have " + Syrup.IRCClient.size() + " IRC client(s) connected:", "LIGHT_CYAN");
						Log.noTS("=======================================","LIGHT_CYAN");
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
					Log.noTS("Unkown command, see /help", "LIGHT_CYAN");

				}
			}
		}
	}
}