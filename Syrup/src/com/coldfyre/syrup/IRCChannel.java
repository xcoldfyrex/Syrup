package com.coldfyre.syrup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class IRCChannel {
	
	public HashMap<String, String> Members = new HashMap<String, String>();
	public List<String> channelMode = new ArrayList<String>();

	private String ChannelName;
	private long ChannelTS;
	private String SID;
	
	public IRCChannel(String ChannelName, long ChannelTS, String SID) {
		this.ChannelName = ChannelName;
		this.ChannelTS = ChannelTS;
		this.SID = SID;
	}
	
	public String getMemberListByUID() {
		String memberlist = "";
		for (String key : Members.keySet()) {
			memberlist = memberlist + " ," + key;
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
		int i = 0;
		String modes = "";
		while (i < channelMode.size()) {
			modes = modes + channelMode.get(i);
			i++;
		}
		return modes;
	}
	
	public String getSID() {
		return SID;
	}

	
	public void setChannelModes(String mode) {
		int i = 0;
		while (i < mode.length()) {
			String modechar = mode.substring(i, i+1);
			if (!modechar.contains("+") && !modechar.contains("-") && !(i == 0)) {
				if (mode.substring(i-1, i).equals("+")) {
					if (!this.channelMode.contains(modechar)) {
						this.channelMode.add(modechar);
					}
				}
				else if (mode.substring(i-1, i).equals("-") || (mode.startsWith("-") && (!mode.contains("+"))))  {
					this.channelMode.remove(modechar);
				}
				else {
					if (!this.channelMode.contains(modechar)) {
						this.channelMode.add(modechar);
					}
				}
				
			}
			i++;
		}
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