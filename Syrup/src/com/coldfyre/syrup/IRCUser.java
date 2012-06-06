package com.coldfyre.syrup;

public class IRCUser {

	public IRCUser(String server, String UID, long signonTime, String nick, String realhost, String hostmask, String ident, String ipaddress, long lastActivity, String modes, String realname) {
		this.UID = UID;
		this.nick = nick;
		this.realname = realname;
		this.ident = ident;
		this.hostmask = hostmask;
		this.realhost = realhost;
		this.ipaddress = ipaddress;
		this.modes = modes;
		this.ident = ident;
		this.realname = realname;
		this.signonTime = signonTime;
		this.lastActivity = lastActivity;
		this.SID = server;
		Syrup.IRCClient.add(this);
	}
	
	/*
	public IRCUser(String nick,String realname,String ident, String hostmask, String vhost, String ipaddress, String modes, String customWhois, boolean isRegistered, boolean isOper, String awayMsg, long signonTime, long lastActivity) {
		this.nick = nick;
		this.realname = realname;
		this.ident = ident;
		this.hostmask = vhost;
		this.realhost = hostmask;
		this.ipaddress = ipaddress;
		this.textModes = modes.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v").replaceAll("[^A-Za-z0-9 ]", "");
		this.modes = textModes.replace("q", "~").replace("a", "&").replace("o", "@").replace("h", "%").replace("v", "+");
		this.customWhois = customWhois;
		this.isRegistered = isRegistered;
		this.isOper = isOper;
		this.awayMsg = awayMsg;
		this.signonTime = signonTime;
		this.lastActivity = lastActivity;
		this.joined = false;
	}
	*/
	
	public void setModes(String mode) {
		this.textModes = mode.replace("~", "q").replace("&", "a").replace("@", "o").replace("%", "h").replace("+", "v").replaceAll("[^A-Za-z0-9 ]", "");
		this.modes = textModes.replace("q", "~").replace("a", "&").replace("o", "@").replace("h", "%").replace("v", "+");
	}
	
	public String getModes() {
		return this.modes;
	}
	

	
	public String getConsoleModes() {
		return this.consoleModes;
	}
	
	public String getConsoleTextModes() {
		return this.consoleTextModes;
	}
	
	
	public String nick,realname,ident,hostmask,realhost,ipaddress,UID,SID;
	private String modes="",textModes="";
	private String consoleModes="", consoleTextModes="";

	//public long lastPingResponse;
	public long signonTime;
	public long lastActivity;
	
	public long getSecondsIdle()
	{
		return (System.currentTimeMillis() - lastActivity) / 1000L;
	}
}