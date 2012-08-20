package com.coldfyre.syrup.Util;

import java.util.HashMap;

@SuppressWarnings("serial")
public class CaseInsensitiveMap extends HashMap<String, Object> {

	public Object put(String key, Object value) {
       return super.put(key.toLowerCase(), value);
    }

	Object get(String key) {
       return super.get(key.toLowerCase());
    }
}

