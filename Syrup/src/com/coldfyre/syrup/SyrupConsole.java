package com.coldfyre.syrup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
					System.out.println("ColdFyre's Syrup IRCD");
					System.out.println("Commands: help, clients, whois <user>, chan <channel>, links, squit <server>, connect, stop");
				} 
				else if (userInputArgs[0].equalsIgnoreCase("SQUIT")) 
				{
					Syrup.WriteSocket("SQUIT " + userInputArgs[1]);
				} 
				else if (userInputArgs[0].equalsIgnoreCase("CONNECT")) 
				{
					Syrup.openConnectorSocket();

				} 
				else if (userInputArgs[0].equalsIgnoreCase("STOP")) 
				{
					System.out.println("Shutting down..");
					Syrup.WriteSocket("SQUIT Shutdown by user");
					System.exit(1);
				}
				else 
				{	
					System.out.println("UNKNOWN COMMAND");

				}
			}
		}
	}
}