package com.coldfyre.syrup;

public class WaffleClient {
	private String RemoteServerID;
	private String RemoteServerHash;
	private int RemoteServerPort;
	private String RemoteServerAddress;
	private String RemoteServerHostname;
	private String RemoteServerName;
	private String RemoveServerVersion;
	
	public WaffleClient() {
		RemoteServerID = "ZZZ";
		RemoteServerHash = "";
		
	}
	
	public String getServerID() {
		return RemoteServerID;
	}
}