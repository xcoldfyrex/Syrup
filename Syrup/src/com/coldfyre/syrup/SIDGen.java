package com.coldfyre.syrup;

public class SIDGen {

	long lastsid = 0;
		
		public String generateSID() {
			char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
			char[] res = new char[2];
			long last = lastsid;
			int len = chars.length;
			for(int i = 1; i > -1; i--) {
				res[i] = chars[(int) (last%len)];
				last = last/len;
			}
			lastsid++;
			// flipover
			if (lastsid == 308915776) lastsid = 0;
			return "9"+(new String(res));
		}
}