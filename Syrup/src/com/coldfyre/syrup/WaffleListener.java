package com.coldfyre.syrup;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class WaffleListener implements Runnable {
    Socket server = null;

	public void run (){
		ServerSocket waffleListner = null;
		try {
			waffleListner = new ServerSocket(6667);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    WaffleClient connection;

        try {
			server = waffleListner.accept();
			System.out.println("[INFO] Got connection from " + server.getRemoteSocketAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        WaffleClient conn_c= new WaffleClient(server);
        Thread t = new Thread(conn_c);
        t.start();
        t.setName("Client - " + server.getRemoteSocketAddress());
	}
}