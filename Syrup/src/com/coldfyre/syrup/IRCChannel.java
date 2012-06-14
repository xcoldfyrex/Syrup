package com.coldfyre.syrup;

import java.util.HashMap;

public class IRCChannel {
	
	private HashMap<String, String> Members = new HashMap<String, String>();
	private String ChannelName = "#dong";
	private long ChannelTS = 0;
	private String ChannelModes = "nt";
	
	public IRCChannel(String ChannelName, long ChannelTS, String ChannelModes) {
		this.ChannelName = ChannelName;
		this.ChannelTS = ChannelTS;
		this.ChannelModes = ChannelModes;
	}


	public void setChannelName(String channelName) {
		ChannelName = channelName;
	}
	
	public String getChannelName() {
		return ChannelName;
	}

	public long getChannelTS() {
		return ChannelTS;
	}


	public void setChannelTS(long channelTS) {
		ChannelTS = channelTS;
	}


	public String getChannelModes() {
		return ChannelModes;
	}


	public void setChannelModes(String channelModes) {
		ChannelModes = channelModes;
	}
	
	public void addUser(String nick, String modes) {
		Members.put(nick, modes);
	}
	
	public int getUserCount() {
		return Members.size();
	}

}