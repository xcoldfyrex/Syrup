package com.coldfyre.syrup;

import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.*;

import com.coldfyre.syrup.UIDGen;
import com.coldfyre.syrup.TS6.UID;
import com.coldfyre.syrup.Util.Config;
import com.coldfyre.syrup.Util.Log;

public class Syrup {

	public static PrintStream out;
	public static BufferedReader in = null;

	public static Socket connectorSocket = null;
	public static Socket linkSocket = null;
	public static boolean running = true;
	public static boolean linkSocketConnected = false;
	public static boolean linkEstablished = false;
	public static boolean sentBurst = false;
	public static boolean sentCapab = false;
	public static boolean debugMode = true;

	// classes
	public static UID UID = new UID();

	static class CriticalSection extends Object {
	}

	static public CriticalSection csIRCClient = new CriticalSection();

	// console thread
	static SyrupConsole syrupConsole = null;
	private static Thread consoleThread = null;

	// pinger thread
	static WafflePING wafflePING = null;
	private static Thread pingThread = null;

	// minecraft server listener thread
	static WaffleListener waffleListener = null;
	private static Thread waffleListenerThread = null;

	private static HashMap<String, IRCUser> IRCClientMap = new HashMap<String, IRCUser>();
	public static HashMap<String, WaffleClient> WaffleClients = new HashMap<String, WaffleClient>();
	public static HashMap<String, IRCChannel> IRCChannels = new HashMap<String, IRCChannel>();
	public static HashMap<String, IRCServer> IRCServers = new HashMap<String, IRCServer>();
	public static HashMap<String, String> WaffleClientsSID = new HashMap<String, String>();

	public static UIDGen uidgen = new UIDGen();

	private static Log log = new Log();

	public static void main(String[] args) throws IOException {

		Log.info(
				"###################### Starting ColdFyre's Syrup IRCD ######################",
				"LIGHT_GREEN");
		Log.info("Reading config..", "LIGHT_GREEN");

		syrupConsole = new SyrupConsole();
		consoleThread = new Thread(syrupConsole);
		consoleThread.start();
		consoleThread.setName("Console Thread");
		Log.info("Started console thread", "LIGHT_GREEN");

		wafflePING = new WafflePING();
		pingThread = new Thread(wafflePING);
		pingThread.start();
		pingThread.setName("PING Thread");

		if (Config.GetProperties()) {
			// if (openConnectorSocket()) {
			//openConnectorSocket();
			//sendCapab();
			// }
			waffleListener = new WaffleListener(Config.localPort);
			waffleListenerThread = new Thread(waffleListener);
			waffleListenerThread.start();
			waffleListenerThread.setName("Waffle Listener Thread");
			Log.info("Started client listener thread", "LIGHT_GREEN");
		}

		while (running) {
			try {
				if (connectorSocket.isClosed() && sentCapab) {
					Log.warn("Connector socket closed " + Config.connectorHost,
							"LIGHT_RED");
					closeConnectorSocket();
				}
			} catch (Exception e) {

			}

			while (linkSocketConnected && connectorSocket != null) {
				try {
					if (in != null) {


						String connectorStream = in.readLine();

						if (connectorStream == null) {
							Log.warn("Lost link to " + Config.connectorHost,
									"LIGHT_RED");
							closeConnectorSocket();
						} else {
							ParseLinkCommand(connectorStream);
						}

						if (!connectorSocket.isConnected()) {
							Log.info("Connector socket lost connection",
									"LIGHT_YELLOW");
						}
					}

				} catch (SocketException e) {
					Log.info("Cannot read from link stream: " + e, "LIGHT_RED");
				}
			}
		}

		Log.info("Exiting main loop, terminating.", "LIGHT_YELLOW");
	}

	private static boolean ShouldGoToWaffles(String data, String UID,
			String Server) {

		if ((WaffleClients.get(Server) != null) && (UID != null)) {
			// Log.warn(UID + " " + Server + " " +
			// WaffleClients.get(Server).RemoteServerID, "");
			if (WaffleClients.get(Server).RemoteServerID != null) {
				if (WaffleClients.get(Server).RemoteServerID
						.equalsIgnoreCase(UID))
					return false; // don't send to self .. from self
			}
		}
		String[] split = data.split(" ");
		WaffleClient searchServer = WaffleClients.get(Server);
		IRCUser searchPlayer = getIRCUser(UID);

		if (split[1].equals("QUIT")) {
			if (searchPlayer.userChannels.size() == 0)
				return false;
			if (searchServer == null) {
				Log.error("Got event from null source server! Data: " + data
						+ " " + UID + " " + Server, "LIGHT_RED");
				WriteSocket(":"
						+ Config.serverName
						+ " PRIVMSG #services :Got event from null source server! Data: "
						+ data + " " + UID + " " + Server);
				return false;
			}

			List<String> intersection = new ArrayList<String>(
					searchServer.userChannels);
			intersection.retainAll(searchPlayer.userChannels);
			if (intersection.size() == 0)
				return false;
		}

		/*
		 * if (split[1].equals("PART")) { if
		 * (searchServer.userChannels.contains(split[2])) return true;
		 * List<String> intersection = new
		 * ArrayList<String>(searchServer.userChannels);
		 * intersection.retainAll(searchPlayer.userChannels); if
		 * (intersection.size() == 0) return false; return false; }
		 */
		if (split[1].equals("PRIVMSG")) {
			// check lobby
			if (searchServer.lobbyChannel.equalsIgnoreCase(split[2]))
				return true;
			// check private message
			String searchUID = searchServer.getUID(split[2]);
			if (searchServer.WaffleIRCClients.containsKey(searchUID))
				return true;
			// check alternate channel
			for (String searchPerson : searchServer.WaffleIRCClients.keySet()) {
				if (searchServer.WaffleIRCClients.get(searchPerson).userChannels
						.contains(split[2]))
					return true;
			}
			return false;

		}
		return true;
	}

	public static void addIRCUser (String name, IRCUser ircuser) {
		if (IRCClientMap.containsKey(name)) return;
		IRCClientMap.put(name, ircuser);
	}
	
	public static void delIRCUser (String name) {
		if (!IRCClientMap.containsKey(name)) return;
		IRCClientMap.remove(name);
	}
	
	public static IRCUser getIRCUser (String name) {
		if (!IRCClientMap.containsKey(name)) return null;
		return IRCClientMap.get(name);
	}
	
	public static HashMap<String, IRCUser> getIRCUserMap() {
		return IRCClientMap;
	}
	
	private static boolean ParseLinkCommand(String data) {
		String[] split = data.split(" ");
		String remoteSID = "";
		String command = "";
		split = data.split(" ");
		if (split[0].startsWith(":")) {
			split[0] = split[0].substring(1);
			remoteSID = split[0];
			command = split[1];
		}
		if (debugMode) {
			if (! Config.debugParams.contains(command)) {
				if (linkEstablished) {
					log.def("[IN] " + data, "");
				} else {
					if(! Config.silentBurst) {
						log.def("[IN] " + data, "");
					}
				}
			}
		}
		if (data.startsWith("ERROR")) {
			Log.error(data, "LIGHT_RED");
			closeConnectorSocket();
		}
		// Got a PING
		if (command.equalsIgnoreCase("PING")) {
			String targetSID = split[3];
			targetSID.replace(":", "");
			if (targetSID.equalsIgnoreCase(Config.SID)) {
				WriteSocket(Config.pre + "PONG " + Config.SID + " " + remoteSID);
			} else {
				WriteSocket(Config.pre + "PONG " + targetSID + " " + remoteSID);
			}
		}
		// KILL
		if (command.equalsIgnoreCase("KILL")) {
			String victom;
			victom = split[2];
			// was a waffle client
			for (String key : WaffleClients.keySet()) {
				if (WaffleClients.get(key).WaffleIRCClients.containsKey(victom)) {
					WriteSocket(":"
							+ split[2].substring(0, 3)
							+ " UID "
							+ split[2]
									+ " "
									+ (System.currentTimeMillis() / 1000L)
									+ " "
									+ WaffleClients.get(key).WaffleIRCClients
									.get(victom).nick
									+ "/mc "
									+ WaffleClients.get(key).WaffleIRCClients
									.get(victom).host
									+ " "
									+ WaffleClients.get(key).WaffleIRCClients
									.get(victom).hostmask
									+ " "
									+ WaffleClients.get(key).WaffleIRCClients
									.get(victom).nick + " 127.0.0.1 "
									+ System.currentTimeMillis() / 1000L
									+ " +r :Minecraft Player");
					WriteSocket(":" + split[2].substring(0, 3)
							+ " FJOIN #minecraft " + System.currentTimeMillis()
							/ 1000L + " +nt :," + split[2]);
					return true;
				}
			}
			String reason;
			reason = Format.join(split, " ", 2);
			if (reason.startsWith(":"))
				reason = reason.substring(1);
			if (getIRCUser(split[2]) != null) {
				WriteWaffleSockets(":" + getIRCUser(split[2]).nick
						+ " QUIT Killed: " + reason, split[0]);
				UID.removeUID(split[2]);
				RemoveFromChannelsByUID(split[0]);
			}
		}

		if (command.equalsIgnoreCase("FMODE")) {
			String mode;
			String modetarget = null;
			String finaltarget = "";
			mode = split[4];
			String source = split[0];
			// source
			if (IRCUser.getNick(source) != null) {
				source = IRCUser.getNick(source);
			}

			// stacked modes, build a list
			if (split.length > 6) {
				for (String key : WaffleClients.keySet()) {
					for (int i = 5; split.length > i; i++) {
						if (getIRCUser(split[i]) != null) {
							modetarget = getIRCUser(split[i]).nick;
						} else if (WaffleClients.get(key).WaffleIRCClients
								.get(split[i]) != null) {
							modetarget = WaffleClients.get(key).WaffleIRCClients
									.get(split[i]).nick + "/mc";
						} else if (split[i].startsWith(":")) {
							modetarget = "";
						} else {
							modetarget = split[i];
						}
						finaltarget = finaltarget + modetarget + " ";
					}
					WriteWaffleSockets(Config.pre + "FMODE " + split[2] + " "
							+ source + " " + mode + " " + finaltarget, null);
				}
			}
			// single user, single mode
			else if (split.length == 6) {
				if (IRCUser.getNick(split[5]) != null) {
					finaltarget = IRCUser.getNick(split[5]);
				}
				WriteWaffleSockets(Config.pre + "FMODE " + split[2] + " "
						+ source + " " + mode + " " + finaltarget, null);
			}
			// stacked modes
			else {
				String target = split[2];
				String newmode = "";
				if (split.length == 5) {
					newmode = split[4];
				} else {
					newmode = split[5] + split[6];
				}
				IRCChannels.get(target).setChannelModes(newmode);
				WriteWaffleSockets(Config.pre + "FMODE " + split[2] + " "
						+ source + " " + mode + " " + finaltarget, null);
			}
		}

		// server we are linked to
		if (split[0].equalsIgnoreCase("SERVER")) {
			IRCServer server;
			server = new IRCServer(split[1], split[1], "", split[4]);
			IRCServers.put(split[2], server);
			Log.info("Introduced server " + split[1], "LIGHT_YELLOW");
		}
		// this is a remote server
		if (command.equalsIgnoreCase("SERVER")) {
			IRCServer server;
			server = new IRCServer(split[2], split[0], "", split[5]);
			IRCServers.put(split[2], server);
			Log.info("Introduced remote server " + split[2], "LIGHT_YELLOW");
		}

		if (command.equalsIgnoreCase("SQUIT")) {
			Log.info("Lost server " + split[2] + " from " + split[0],
					"LIGHT_YELLOW");
			String SID = IRCServers.get(split[2]).SID;
			purgeUIDByServer(SID);
			IRCServers.remove(split[2]);

		}

		if (command.equalsIgnoreCase("MODE")) {
			String target, newmode;
			// sender = split[0];
			target = split[2];
			if (split.length == 4) {
				newmode = split[3];
			} else if (split.length == 5) {
				newmode = split[4];
			} else {
				newmode = split[4] + split[5];
			}
			getIRCUser(target).setServerModes(newmode);
		}

		if (command.equalsIgnoreCase("FJOIN")) {
			String chanserv = split[0];
			String channame = split[2];
			long chanTS = Long.parseLong(split[3]);
			String chanmodes = split[4];
			int i = 0;
			while (i < 5) {
				split[i] = "";
				i++;
			}

			String[] people = data.split(" ");
			for (String user : people) {
				if (!user.contains(","))
					continue;
				String infoz[] = user.split(",");
				// channel exists, just add people
				if (IRCChannels.get(channame) != null) {
					IRCChannels.get(channame).addUser(infoz[1], infoz[0]);
					getIRCUser(infoz[1]).addChannel(channame);
				}
				// is new chan
				else {
					IRCChannel channel = new IRCChannel(channame, chanTS,
							chanserv);
					IRCChannels.put(channame, channel);
					IRCChannels.get(channame).setChannelModes(chanmodes);
					IRCChannels.get(channame).addUser(infoz[1], infoz[0]);
					getIRCUser(infoz[1]).addChannel(channame);
				}
				WriteWaffleSockets(Config.pre + "FJOIN " + channame + " ,"
						+ getIRCUser(infoz[1]).nick, null);
			}
		}

		if (command.equalsIgnoreCase("UID")) {
			UID.add(split);
			WriteWaffleSockets(Config.pre + "UID " + split[4] + " " + split[7]
					+ " " + split[6], null);
		}

		if (command.equalsIgnoreCase("ENDBURST")) {
			Log.info("Succesfully linked", "LIGHT_GREEN");
			linkEstablished = true;
		} 

		if (command.equalsIgnoreCase("PART")) {
			getIRCUser(split[0]).removeChannel(split[2]);
			String reason;
			reason = Format.join(split, " ", 3);
			if (reason.startsWith(":"))
				reason = reason.substring(1);
			WriteWaffleSockets(":" + getIRCUser(split[0]).nick + " PART "
					+ split[2] + " " + reason, split[0]);
			RemoveFromChannelsByUID(split[0]);
		}

		if (command.equalsIgnoreCase("KICK")) {
			for (String key : WaffleClients.keySet()) {
				if (WaffleClients.get(key).WaffleIRCClients
						.containsKey(split[3])) {
					// is a WaffleIRC client, rejoin them
					WriteSocket(":" + split[3].substring(0, 3) + " FJOIN "
							+ split[2] + " " + System.currentTimeMillis()
							/ 1000L + " +nt :," + split[3]);
					return true;
				}
			}
			// regular IRC person
			String target = split[3];
			if (split[0].startsWith(":"))
				split[0] = split[0].substring(1);
			String kicker = split[0];
			if (split[0].length() == 3) {
				for (String server : IRCServers.keySet()) {
					// Log.warn(server + " " + IRCServers.get(server).SID + " "
					// + kicker, "LIGHT_RED");
					if (IRCServers.get(server).SID.equalsIgnoreCase(split[0]))
						kicker = IRCServers.get(server).servername;
				}

			} else {
				kicker = getIRCUser(split[0]).nick;
			}

			try {
				getIRCUser(target).removeChannel(split[2]);
			} catch (java.lang.NullPointerException e) {
				Log.error("TRIED TO RMEOVE " + target + " FROM " + split[2]
						+ " " + e + " ", "LIGHT_RED");
				return false;
			}
			String reason;
			reason = Format.join(split, " ", 4);
			if (reason.startsWith(":"))
				reason = reason.substring(1);
			WriteWaffleSockets(":" + kicker + " KICK " + split[2] + " "
					+ getIRCUser(target).nick + " " + reason, split[0]);
			RemoveFromChannelsByUID(split[0]);
		}

		if (command.equalsIgnoreCase("FHOST")) {
			WriteWaffleSockets(":" + getIRCUser(split[0]).nick + " FHOST "
					+ split[2].substring(1), split[0]);
			RemoveFromChannelsByUID(split[0]);
		}

		if (command.equalsIgnoreCase("QUIT")) {
			String reason;
			reason = Format.join(split, " ", 2);
			if (reason.startsWith(":"))
				reason = reason.substring(1);
			WriteWaffleSockets(":" + getIRCUser(split[0]).nick + " QUIT "
					+ reason, split[0]);
			UID.removeUID(split[0]);
			RemoveFromChannelsByUID(split[0]);
		}

		if (command.startsWith("NICK")) {
			WriteWaffleSockets(":" + getIRCUser(split[0]).nick + " NICK "
					+ split[2], split[0]);
			getIRCUser(split[0]).nick = split[2];
		}

		if (data.equalsIgnoreCase("CAPAB START 1202")) {
			// sendCapab();
		}

		if (command.startsWith("PRIVMSG")) {
			if (split[3].startsWith(":"))
				split[3] = split[3].substring(1);
			if (split[2].startsWith(":"))
				split[2] = split[2].substring(1);
			if (split[0].startsWith(":"))
				split[0] = split[0].substring(1);
			String message = Format.join(split, " ", 3);
			String source = IRCUser.getNick(split[0]);
			String target = split[2];
			if (message.equalsIgnoreCase("VERSION")) {

			}

			else {
				if (!target.startsWith("#")) {
					for (String key : WaffleClients.keySet()) {

						if (WaffleClients.get(key).WaffleIRCClients
								.get(split[2]) != null) {
							target = WaffleClients.get(key).WaffleIRCClients
									.get(split[2]).nick;
							WriteWaffleSockets(":" + source + " PRIVMSG "
									+ target + " :" + message, split[0]);
						}
					}
				} else {
					WriteWaffleSockets(":" + source + " PRIVMSG " + target
							+ " :" + message, split[0]);
				}
			}
		}

		if (command.startsWith("IDLE")) {
			WriteSocket(":" + split[2] + " IDLE " + split[0] + " 0 0");
		}

		return true;
	}

 	private static void RemoveFromChannelsByUID(String UID) {
		for (String key : Syrup.IRCChannels.keySet()) {
			IRCChannel channel;
			channel = Syrup.IRCChannels.get(key);
			channel.removeUserByUID(UID);
		}
	}

 	private static void RemoveFromChannelsBySID(String SID) {
		for (String key : Syrup.IRCChannels.keySet()) {
			IRCChannel channel;
			channel = Syrup.IRCChannels.get(key);
			channel.removeUserBySID(SID);
		}
	}

 	public static void WriteWaffleSockets(String data, String UID) {
		for (String key : Syrup.WaffleClients.keySet()) {
			WaffleClient link;
			link = Syrup.WaffleClients.get(key);
			if (link != null) {
				if (ShouldGoToWaffles(data, UID, key)) {
					WaffleClients.get(key).WriteSocket(data);
				}
			}
		}
	}

	public static void ConnectLink() {
		if (linkEstablished) {
			Log.warn("Link already established, please SQUIT", "LIGHT_RED");
			return;
		}

		if (openConnectorSocket()) {
			SendCapab();
			if (sentCapab) {
				SendBurst();
			}
		} else {
			Log.error("Failed to open connector socket", "LIGHT_RED");
			return;
		}
	}

	private static void SendCapab() {
		WriteSocket("CAPAB START 1201");
		WriteSocket("CAPAB CAPABILITIES :NICKMAX=33 CHANMAX=50 IDENTMAX=33 MAXTOPIC=500 MAXQUIT=500 MAXKICK=500 MAXGECOS=500 MAXAWAY=999 MAXMODES=1 HALFOP=1 PROTOCOL=1201");
		WriteSocket("CAPAB END");
		WriteSocket("SERVER " + Config.serverName + " " + Config.linkPassword
				+ " 0 " + Config.SID + " :Syrup");
		sentCapab = true;
	}

	private static boolean SendBurst() {
		WriteSocket(Config.pre + "BURST "
				+ (System.currentTimeMillis() / 1000L));
		WriteSocket(Config.pre + "VERSION : Syrup");
		// out.println(pre+"UID 1SYAAAAAA "+(System.currentTimeMillis() /
		// 1000L)+" syrup.paradoxirc.net syrup.paradoxirc.net syrup.paradoxirc.net syrup.paradoxirc.net 127.0.0.1 "+(System.currentTimeMillis()
		// / 1000L)+" +Siosw +ACKNOQcdfgklnoqtx : PONY");
		// out.println(":1SYAAAAAA OPERTYPE NetAdmin");
		WriteSocket(Config.pre + "ENDBURST");
		sentBurst = true;
		return true;
	}

	public static void WriteSocket(String data) {
		if (debugMode) {
			String[] command = data.split(" ");
			if (! Config.debugParams.contains(command[1])) {
				if (linkEstablished) {
					log.def("[OUT] " + data, "");
				} else {
					if(! Config.silentBurst) {
						log.def("[OUT] " + data, "");
					}
				}
			}
		}
		out.println(data);
	}

	private static boolean openConnectorSocket() {
		if (linkSocketConnected) {
			Log.error("Somehow tried to open connector socket twice?",
					"LIGHT_RED");
			return true;
		}

		if (connectorSocket != null) {
			try {
				connectorSocket.close();
			} catch (IOException e) {
			}
			connectorSocket = null;
		}
		Log.info("Connecting to server: " + Config.connectorHost, "LIGHT_GREEN");
		sentBurst = false;
		sentCapab = false;
		try {
			connectorSocket = new Socket(Config.connectorHost,
					Config.connectorPort);

		} catch (UnknownHostException e) {
			Log.error("DNS Failure", "LIGHT_RED");
			return false;
		} catch (IOException e) {
			Log.error("Can't connect to: " + Config.connectorHost + " Reason:"
					+ e, "LIGHT_RED");
			return false;
		}
		if (connectorSocket == null) {
			Log.error("Failed connect to: " + Config.connectorHost, "LIGHT_RED");
			return false;
		}
		Log.info("Connected to: " + Config.connectorHost, "LIGHT_GREEN");
		Log.info("Socket info: " + connectorSocket.getInetAddress(),
				"LIGHT_GREEN");
		linkSocketConnected = true;
		linkSocket = connectorSocket;
		try {
			in = new BufferedReader(new InputStreamReader(
					linkSocket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			out = new PrintStream(linkSocket.getOutputStream(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	public static boolean closeConnectorSocket() {
		Log.info("Shutdown connector socket", "LIGHT_YELLOW");
		linkSocketConnected = false;
		sentBurst = false;
		sentCapab = false;
		linkEstablished = false;
		if ((connectorSocket != null) && connectorSocket.isConnected()) {
			try {
				connectorSocket.close();
			} catch (IOException e) {
				Log.info("Caught exception, closing connector: " + e,
						"LIGHT_YELLOW");
			}
		}
		return true;
	}

	private static int getWaffleClientServerName(String servername) {
		int i = 0;
		String sname;
		while (i < WaffleClients.size()) {
			sname = WaffleClients.get(i).RemoteServerName;
			System.out.println("LIST: " + i + " " + sname + " " + servername);
			if (sname.equalsIgnoreCase(servername)) {
				return i;
			} else
				i++;
		}
		return -1;
	}

	private static void purgeUIDByServer(String SID) {
		for (Iterator<Map.Entry<String, IRCUser>> i = getIRCUserMap().entrySet()
				.iterator(); i.hasNext();) {
			Map.Entry<String, IRCUser> entry = i.next();
			if (entry.getKey().startsWith(SID)) {
				Log.info("Lost client " + entry.getKey() + " from " + SID
						+ " netsplit", "LIGHT_YELLOW");
				WriteWaffleSockets(":" + getIRCUser(entry.getKey()).nick
						+ " QUIT " + "*.net *.split", entry.getKey());
				RemoveFromChannelsByUID(entry.getKey());
				i.remove();
			}
		}
	}
}