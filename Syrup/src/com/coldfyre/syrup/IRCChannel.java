package com.coldfyre.syrup;

public class IRCChannel {
	
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

}