package com.klst.iban;

import java.util.logging.Logger;

import org.apache.commons.validator.routines.IBANValidator;

import com.klst.iban.Result.BankData;

public class InternationalBankAccountNumber {

	private static final Logger LOG = Logger.getLogger(InternationalBankAccountNumber.class.getName());
	
	static boolean isValid(String iban) {
		IBANValidator validator = IBANValidator.getInstance();
		return validator.isValid(iban);
	}
	
	String iban = null;
	boolean isValid = false;
	String format = null;
	
	public InternationalBankAccountNumber(String iban) {
		if(iban!=null) {
			this.iban = iban.trim();
			
			isValid = isValid(this.iban);
			if(isValid) {
				LOG.info(this.iban + " is valid.");
			} else {
				LOG.warning(this.iban + " is not valid.");
			}
		}
	}
	
	public String getCountryCode() {
		return isValid ? iban.substring(0, 2) : null;
	}
	
    public String getCheckDigits() {
		return isValid ? iban.substring(2, 4) : null;
    }
    
    public String getBbanPart() {
    	return isValid ? iban.substring(4) : null;
    }
    
    // bankCode + branchCode property
    public String getBankDataPart() {
    	if(!isValid) return null;
    	BankData bankData = getBankData();
    	String bankDataPart = bankData.getBankIdentifier();
    	Bban bData = Bban.BBAN.get(getCountryCode());
    	return bankDataPart + bData.getGroupBranchCode(iban);
    }

    public BankData getBankData() {
    	if(!isValid) return null;
		if(Bban.BBAN.get(getCountryCode())==null) {
			LOG.warning("No BBAN information for CountryCode "+getCountryCode());
			return null;
		}
		Bban bData = Bban.BBAN.get(getCountryCode()); // liefert eine Instanz mit Methode getBankData
		format = bData.format;
//		LOG.info(bData.getBankData(iban).toString());
		return bData.getBankData(iban);
    }
}
