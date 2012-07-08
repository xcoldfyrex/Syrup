package com.coldfyre.syrup;

public class WafflePING implements Runnable {
	
	public void run() {
		while (1 == 1){
			int i = 3;
			while (i < Syrup.WaffleClients.size()) {
				long now = System.currentTimeMillis() / 1000L;
				if ((now - Syrup.WaffleClients.get(i).LastPong) >= 60){
					if (!(Syrup.WaffleClients.get(i).LastPong == 0)) {
						System.out.println(System.currentTimeMillis() / 1000L + " " + Syrup.WaffleClients.get(i).LastPong);
						Syrup.WaffleClients.get(i).CloseSocket("Ping timeout: " + (System.currentTimeMillis() / 1000L - Syrup.WaffleClients.get(i).LastPong + " seconds"));
					}
					i++;
				}
				if (i >= Syrup.WaffleClients.size()) continue;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
}