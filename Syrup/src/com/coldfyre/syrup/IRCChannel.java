package com.coldfyre.syrup;

import java.util.HashMap;


public class IRCChannel {
	
	public HashMap<String, String> Members = new HashMap<String, String>();
	private String ChannelName;
	private long ChannelTS;
	private String ChannelModes;
	private String SID;
	
	public IRCChannel(String ChannelName, long ChannelTS, String ChannelModes, String SID) {
		this.ChannelName = ChannelName;
		this.ChannelTS = ChannelTS;
		this.ChannelModes = ChannelModes;
		this.SID = SID;
	}
	
	public String getMemberListByUID() {
		String memberlist = "";
		for (String key : Members.keySet()) {
			memberlist = memberlist + " ," + key ;
		}
		return memberlist;
	}
	
	public String getMemberListByNick() {
		String memberlist = "";
		for (String key : Members.keySet()) {
			for (String keytoo : Syrup.IRCClient.keySet()) {
				if (key.equals(keytoo)) {
					IRCUser person;
					person = Syrup.IRCClient.get(key);
					memberlist = memberlist + " ," + person.nick;
				}
	    	}
		}
		return memberlist;
	}
	
	public String getChannelMemberInfo(String person) {
		return Members.get(person);
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
	
	public String getSID() {
		return SID;
	}


	public void setChannelModes(String channelModes) {
		ChannelModes = channelModes;
	}
	
	public void addUser(String nick, String modes) {
		Members.put(nick, modes);
	}
	
	public void removeUserByUID(String nick) {
		if (Members.get(nick) != null) Members.remove(nick);
	}
	
	public void removeUserBySID(String nick) {
		for (String key : Members.keySet()) {
			if (key.startsWith(nick)) { Members.remove(key); }
		}
	}
	
	public int getUserCount() {
		return Members.size();
	}

}