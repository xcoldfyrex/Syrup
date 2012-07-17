package com.coldfyre.syrup;

import java.util.ArrayList;
import java.util.List;

import com.coldfyre.syrup.Util.Config;

public class WaffleIRCClient {

	public List<String> userChannels = new ArrayList<String>();
	
	public WaffleIRCClient(String nick, String host, boolean cloaked, String SID, long signon) {
		this.nick = nick;
		this.host = host;
		this.SID = SID;
		String[] hostsplit = host.split("\\.");
		String hiddenhost = "hidden.host";
		if (host.matches("(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?).(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")) {
			hiddenhost = "hidden."+hostsplit[1]+"-"+hostsplit[0]+"-IN-ADDR.ARPA";
		} 
		else {
			String joined = "";
			for (int i = 1; hostsplit.length>i; i++ ){
				joined=joined+"."+hostsplit[i];
			}
			hiddenhost = "hidden"+joined ;
		}
		this.hostmask = hiddenhost;
		
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
	
	public String nick = "Guest";
	public String host = "unknown";
	public String hostmask = "hidden.host";
	public boolean cloaked = false;
	public String SID = Config.SID;
	public long signon = System.currentTimeMillis() / 1000L;
	
	
}