package com.coldfyre.syrup;

public class IRCServer {
	public IRCServer(String servername, String parent, String version ,String SID) {
		this.servername = servername;
		this.parent = parent;
		this.version = version;
		this.SID = SID;		
	}
	
	public String SID,version,parent,servername;

}
