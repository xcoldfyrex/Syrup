package com.coldfyre.syrup.TS6;

import com.coldfyre.syrup.Format;
import com.coldfyre.syrup.IRCUser;
import com.coldfyre.syrup.Syrup;

public class UID {
	public void add(String[] UIDString) {
		long idleTime = Long.parseLong(UIDString[3]) * 1000;
		long signedOn = Long.parseLong(UIDString[9]);
		if (UIDString[11].startsWith(":")) UIDString[11] = UIDString[11].substring(1);
		String realname = Format.join(UIDString, " ", 11);
		synchronized(Syrup.csIRCClient) {
			IRCUser ircuser = new IRCUser(UIDString[1],UIDString[2],idleTime, UIDString[4],UIDString[5],UIDString[6],UIDString[7],UIDString[8],signedOn, "", realname);
			ircuser.setServerModes(UIDString[10]);
		}
		/* TODO
		 *  send UID to clients
		 */
	}
	
	
	public void updateUID(String UID) {
		
	}
	
	public void removeUID(String UID) {
		Syrup.IRCClient.remove(UID);
	}
	


	
	
	
}