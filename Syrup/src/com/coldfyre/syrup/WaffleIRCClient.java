package com.coldfyre.syrup;

public class WaffleIRCClient {
	public WaffleIRCClient(String nick, String host, boolean cloaked, String SID, long signon) {
		this.nick = nick;
		this.host = host;
		this.SID = SID;
		
	}
	public String nick = "Guest";
	public String host = "unknown";
	public boolean cloasked = false;
	public String SID = Syrup.SID;
	public long signon = System.currentTimeMillis() / 1000L;
	
	
}