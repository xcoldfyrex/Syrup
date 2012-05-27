package com.coldfyre.syrup;

public class Format {
	
	public static String join(String[] strArray, String delimiter, int start) {
		String joined = "";
		int noOfItems = 0;
		for (String item : strArray) {
			if (noOfItems < start) { noOfItems++; continue; }
			joined += item;
			if (++noOfItems < strArray.length)
			joined += delimiter;
		}
		return joined;
	}
}