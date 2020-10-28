package com.klst.iban;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.klst.iban.Result.BankData;

public class IbanToBankProxy implements IbanBankData {

	private static final Logger LOG = Logger.getLogger(IbanToBankProxy.class.getName());
	
	private IbanToBankData ibanToBankData; // the real subject
	FileProxy proxy;
	
	public IbanToBankProxy() {
		this(null);
	}
	
	public IbanToBankProxy(String api_key) {
		if(api_key==null) {
			ibanToBankData = null;
		} else {
			ibanToBankData = new IbanToBankData(api_key);
		}
		setProxy(new LocalFileProxy()); // default
	}

	public void setProxy(FileProxy fp) {
		proxy = fp;
	}
	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * The lookup for BankData is done in the proxy data store (file or database).
	 * If not found and api_key for iban.com/iban-checker exists, the bank data is retrieved from there.
	 * <br>
	 * For invalid ibans the result is null.
	 */
	@Override
	public BankData getBankData(String iban) {
		InternationalBankAccountNumber ibanNumber = new InternationalBankAccountNumber(iban);
		if(!ibanNumber.isValid) return null;
		
		String bankDataPart = ibanNumber.getBankDataPart();
		String countryCode = ibanNumber.getCountryCode();
		String bankIdentifier = ibanNumber.getBankData().getBankIdentifier();
		Object branchCode = ibanNumber.getBankData().getBranchCode();
		LOG.info(countryCode + " / " + bankIdentifier + " " + branchCode + " = " + bankDataPart);
		return getBankData(ibanNumber.iban, countryCode, bankIdentifier, branchCode);
	}

	private BankData getBankData(String iban, String countryCode, String bankIdentifier, Object branchCode) {
		Map<Long, JSONObject> countryBanks = new Hashtable<Long, JSONObject>();
		
		String jsonString = proxy.loadFile(countryCode);
		// no proxy for country ==> do real subject
		if(jsonString==null) {
			if(ibanToBankData==null) {
				LOG.warning("No access to iban.com API.");
				return null;
			} else {
				return ibanToBankData.getBankData(iban);
			}
		}
		
	        JSONParser jsonParser = new JSONParser();
	        Object o = null;
			try {
				o = jsonParser.parse(jsonString);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // throws ParseException
	        JSONObject jo = (JSONObject) o;
	        Object country_code = jo.get("country_code");
	        LOG.info("country_code:"+(String)country_code); 
	        Object isSEPAcountry = jo.get("isSEPAcountry");
	        LOG.info("isSEPAcountry:"+(Boolean)isSEPAcountry); 
	        Object ISO_3166_1 = jo.get("ISO_3166_1");
	        if(ISO_3166_1 instanceof JSONObject) {
	        	JSONObject jISO_3166_1 = (JSONObject)ISO_3166_1;
		        LOG.info("ISO_3166_1.name:"+(String)jISO_3166_1.get("name")
		        	+ ", name_fr:"+(String)jISO_3166_1.get("name_fr")
		        	); 
	        }
	        Object list = jo.get("list");
	        if(list instanceof JSONArray) {
	        	List<JSONObject> array = (JSONArray)list;
	        	LOG.info("JSONArray.size="+array.size());
	        	Iterator iter = array.listIterator();
	        	while(iter.hasNext()) {
//	        		BankDataJSONObject bdo = new BankDataJSONObject((JSONObject)iter.next());
	        		JSONObject jEntry = (JSONObject)iter.next();
	        		LOG.info(""+BankDataOrdered.toOrderedJSONString(jEntry));
	        		countryBanks.put((Long)jEntry.get(Bank_Data.ID), jEntry);
	        	}
//	        	array.forEach(entry -> {
//	        		LOG.config(""+(BankDataJSONObject)entry);
//	        		JSONObject jEntry = (JSONObject)entry;
//			        LOG.fine("id:"+(Long)jEntry.get("id")
//		        		+ ", swift_code:"+(String)jEntry.get("swift_code")
//		        		+ ", ... support_codes:"+(Long)jEntry.get("support_codes")
//			        );
//			        countryBanks.put((Long)jEntry.get("id"), jEntry);
//	        	});
	        	
	        }
			LOG.info(bankIdentifier + "+" + branchCode + " in countryBanks suchen ...");
			// key aka id
			// AT:Long.parseLong(bankDataPart, 10);
			// BG:Long.parseLong(branchCode, 10); da bankBankIdentifier alpha ist, Bsp: UNCR9660 TODO
			Long key = null;
			try {
				Long bankId = BankId.getBankId(countryCode, bankIdentifier, branchCode);
				key = bankId;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			Long key = Long.parseLong(bankDataPart, 10);
			LOG.info("key=" + key + " suchen in #"+countryBanks.size());
//			Long key = Long.getLong(bankDataPart);
			JSONObject entry = countryBanks.get(key);
			LOG.info(""+entry);
			// TODO bei AD immer BranchCode:0000
			if(entry==null) {
				// in proxy nix gefunden
				if(ibanToBankData==null) {
					LOG.warning("No proxy data for "+iban+". No iban.com API Key provided.");
					return null;
				} else {
					return ibanToBankData.getBankData(iban);
				}			
			}
			return parseBankDataObject(entry);
			
	}
	
	BankData parseBankDataObject(JSONObject bank_data) {
		BankData bankData = new BankData();
		// mandatory props:
		bankData.setBic((String)bank_data.get(Bank_Data.SWIFT_CODE)); // in proxy data I use SWIFT_CODE instead of BIC
		bankData.setBank((String)bank_data.get(IbanToBankData.BANK)); // aka bank name
		bankData.setCity((String)bank_data.get(IbanToBankData.CITY));
		// in proxy data BANK_CODE can be int/numeric or string
		Object value = bank_data.get(IbanToBankData.BANK_CODE);
        try {
        	int bc = Integer.parseInt(value.toString());
        	bankData.setBankCode(bc);
        } catch (NumberFormatException e) {
        	LOG.fine(IbanToBankData.BANK_CODE+" "+value + " is not numeric.");
        	bankData.setBankIdentifier(value.toString());
        }        

		// optional:
		try {
			bankData = getOptionalKey(bank_data, IbanToBankData.BRANCH, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.BRANCH_CODE, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.ADDRESS, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.STATE, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.ZIP, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.PHONE, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.FAX, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.WWW, bankData);
			bankData = getOptionalKey(bank_data, IbanToBankData.EMAIL, bankData);
			bankData = getOptionalKey(bank_data, Bank_Data.SUPPORT_CODES, bankData);
		} catch (Exception e) {
			LOG.severe(e.getMessage());
//			e.printStackTrace();
		}
		// country, country_iso
		// account
		LOG.info(bankData.toString());
		return bankData;
	}

 	private BankData getOptionalKey(JSONObject bank_data, String key, BankData bankData) throws Exception {
 		if(key.equals(IbanToBankData.BRANCH)) {
 			// implement client responsibility to get numeric branch code:
 			Object value = bank_data.get(key);
 			if(value==null) {
 				return bankData;	
 			} 
            try {
//            	int bc = Integer.parseInt(value.toString());
            	bankData.branchCode = new Integer(Integer.parseInt(value.toString())); 
            } catch (NumberFormatException e) {
            	LOG.fine(key+" "+value + " is not numeric.");
            }        
			return bankData;
		} else {
			return Bank_Data.getOptionalKey(bank_data, key, bankData);
		}
	}

}
