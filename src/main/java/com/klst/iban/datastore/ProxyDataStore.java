package com.klst.iban;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

/*

- file pro countryCode ein file
-- local file
-- remote file (in git)
-- database

 */
public class ProxyDataStore {

	// TODO
}

class FileProxy extends ProxyDataStore {

	// TODO
	
	/**
	 * 
	 * @param countryCode
	 * @return
	 */
	String loadFile(String countryCode) {
		return null;	
	}
}

class LocalFileProxy extends FileProxy {

	private static final Logger LOG = Logger.getLogger(LocalFileProxy.class.getName());
	static final String RESOURCE_PATH = "iban.data/iban-countries/";

	// TODO
	
	String loadFile(String countryCode) {
		File file = new File(RESOURCE_PATH+countryCode+".json");
        BufferedReader in;
		if(file.exists()) {
			LOG.info(file.getAbsolutePath() + " exists.");
			// laden in Map<Long, JSONObject> countryBanks :: mit key = id

	        String inputLine;
	        StringBuffer response = new StringBuffer();
			try {
				in = new BufferedReader(new FileReader(file));
		        while ((inputLine = in.readLine()) != null) {
		            response.append(inputLine);
		        }
		        in.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        String jsonString = response.toString();
	        LOG.info("response:\n"+jsonString.length()+"<");
	        return jsonString;
		} else {
			LOG.warning("No proxy provider for "+countryCode+" exists in "+RESOURCE_PATH+".");
	        return null;
		}
	}
}

class RemoteFileProxy extends FileProxy {

	private static final Logger LOG = Logger.getLogger(RemoteFileProxy.class.getName());
	static final String URL_PREFIX = "https://raw.githubusercontent.com/homebeaver/bankdata/main/iban-countries/";

	// TODO
	
	/*
	 * 
	 * TODO @return empty String if no proxy file exists ==> return null
	 */
	String loadFile(String countryCode) {
        BufferedReader in;
        String inputLine;
        StringBuffer response = new StringBuffer();
		try {
			URL url = new URL(URL_PREFIX+countryCode+".json");
			in = new BufferedReader(new InputStreamReader(url.openStream())); // , "UTF-8"
			LOG.info(">>>>>>>>>>>>>>>>> Open url:"+url);
	        while ((inputLine = in.readLine()) != null) {
	            response.append(inputLine);
	        }
			in.close();
		} catch (MalformedURLException e) {
			LOG.warning("MalformedURLException:" + e.getMessage());
//			e.printStackTrace();
		} catch (IOException e) {
			LOG.warning("No proxy found at url "+e.getMessage());
			return null;
		}
        String jsonString = response.toString();
        if(jsonString.isEmpty()) {
			LOG.warning("No proxy provider for "+countryCode+" exists in remote data store.");
        	return null;
        }
        LOG.info("response length="+jsonString.length()+"<");
        return jsonString;

	}
	
}
