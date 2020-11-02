package com.klst.iban.datastore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

public class RemoteFileProxy extends FileProxy {

	private static final Logger LOG = Logger.getLogger(RemoteFileProxy.class.getName());
	static final String URL_PREFIX = "https://raw.githubusercontent.com/homebeaver/bankdata/main/iban-countries/";

	// TODO
	
	/*
	 * 
	 * TODO @return empty String if no proxy file exists ==> return null
	 */
	public String loadFile(String countryCode) {
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
