package com.klst.iban.datastore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

public class LocalFileProxy extends FileProxy {

	private static final Logger LOG = Logger.getLogger(LocalFileProxy.class.getName());
	public static final String RESOURCE_DATA_PATH = "data/";
	static final String RESOURCE_PATH = RESOURCE_DATA_PATH + "iban-countries/";

	public static String loadFile(File file) {
        BufferedReader in = null;
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
//			} finally {
//				in.close();
			}
	        String jsonString = response.toString();
	        LOG.config("jsonString.length is "+jsonString.length());
	        return jsonString;
		} else {
			LOG.warning("Not exists file:"+file.getAbsolutePath()+".");
	        return null;
		}
		
	}
	
	// TODO
	
	public String loadFile(String countryCode) {
		File file = new File(RESOURCE_PATH+countryCode+".json");
		if(file.exists()) {
			return loadFile(file);
		} else {
			LOG.warning("No proxy provider for "+countryCode+" exists in "+RESOURCE_PATH+".");
	        return null;
		}
	}
}
