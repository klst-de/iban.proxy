package com.klst.iban;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/*

Es gibt keine unique BankId. Weder weltweit, noch für ein Land (CountryCode).

Dies ist der Versuch aus properties bankCode, branchCode eine Id zu berechnen.
Pro CountryCode gibt es ein anderes Verfahren. Also gibt es mehrere idFunctions:

- a : alpha bankCode, nil branch, Bsp NL
- b : numeric bankCode, numeric branchCode, Bsp GR (nicht AD, das liefert: bankCode+0000 da jeder branchCode gültig ist)
...

 */
// aus Java8BiFunction1 https://mkyong.com/java8/java-8-bifunction-examples/
public class BankId {

	private static final Logger LOG = Logger.getLogger(BankId.class.getName());
	
	private static BankId instance = null; // SINGLETON

	Map<String, Character> countryToFunc = new HashMap<>();

	private BankId() {
    	// sepa countries
    	countryToFunc.put("AD", BANKCODE_WITH_ZERO_BRANCHCODE);
    	countryToFunc.put("AT", NUMERIC_BANKCODE);
    	countryToFunc.put("BE", NUMERIC_BANKCODE);
    	countryToFunc.put("BG", SORTCODE_LIKE);
    	countryToFunc.put("CH", NUMERIC_BANKCODE);
    	countryToFunc.put("CY", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("CZ", NUMERIC_BANKCODE);
    	countryToFunc.put("DE", NUMERIC_BANKCODE);
    	countryToFunc.put("DK", NUMERIC_BANKCODE);
    	countryToFunc.put("EE", NUMERIC_BANKCODE);
    	countryToFunc.put("ES", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("FI", NUMERIC_BANKCODE);
    	countryToFunc.put("FR", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("GB", SORTCODE_LIKE);
    	countryToFunc.put("GI", ALPHA_BANKCODE);
    	countryToFunc.put("GR", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("HR", NUMERIC_BANKCODE);
    	countryToFunc.put("HU", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("IE", SORTCODE_LIKE);
    	countryToFunc.put("IS", NUMERIC_BANKCODE);
    	countryToFunc.put("IT", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("LI", NUMERIC_BANKCODE);
    	countryToFunc.put("LT", NUMERIC_BANKCODE);
    	countryToFunc.put("LU", NUMERIC_BANKCODE);
    	countryToFunc.put("LV", ALPHA_BANKCODE);
    	countryToFunc.put("MC", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("MT", SORTCODE_LIKE); // 4!a5!n
    	countryToFunc.put("NL", ALPHA_BANKCODE);
    	countryToFunc.put("NO", NUMERIC_BANKCODE);
    	countryToFunc.put("PL", NUMERIC_BANKCODE);
    	countryToFunc.put("PT", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("RO", ALPHA_BANKCODE);
    	countryToFunc.put("SE", NUMERIC_BANKCODE);
    	countryToFunc.put("SI", NUMERIC_BANKCODE);
    	countryToFunc.put("SK", NUMERIC_BANKCODE);
    	countryToFunc.put("SM", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("VA", NUMERIC_BANKCODE);
	}

	public static BankId getInstance() {
		if (instance == null) {
			instance = new BankId();
		}
		return instance;
	}
	
	/**
	 * calculates a BankId unique per country
	 * 
	 * @param countryCode - two letters
	 * @param bankCode - the BankIdentifier String found in an iban
	 * @param branchCode - the branch code Object found in an iban, can be null
	 * 
	 * @return a Long country unique id for Bank with bankCode and branchCode
	 * @throws Exception 
	 */
    public static Long getBankId(String countryCode, String bankCode, Object branchCode) throws Exception {
    	BankId bankId = getInstance();
    	Character cmd = bankId.countryToFunc.get(countryCode);
    	if(cmd==null) {
    		throw new Exception("No BankId Function defined for country "+countryCode);
    	}
    	return idFunc.get(cmd).apply(bankCode, branchCode);
    }
    
    static final Character ALPHA_BANKCODE = 'a'; // example: NL91 ABNA 0417164300
    static Long a(String bankCode, Object ignored) {
    	// branchC wird ignoriert
    	return (long)BusinessIdentifierCode.bankCodeToId(bankCode);
    }

    static final Character BANKCODE_WITH_ZERO_BRANCHCODE = 'z'; // example: AD86 0004 0019 000140145012
    static Long z(String bankCode, Object branchCode) {
    	LOG.fine(bankCode + " "+ branchCode);
    	String bankDataPart = new String(bankCode + "0000"); // vorerst feste Länge
    	return Long.parseLong(bankDataPart, 10);
    }

    static final Character BANKCODE_AND_BRANCHCODE_NUMERIC = 'b'; // example: GR16 011 0125 0000000012300695
    static Long b(String bankCode, Object branchCode) {
    	String bankDataPart = new String(bankCode + branchCode.toString());
    	return Long.parseLong(bankDataPart, 10);
    }

    static final Character NUMERIC_BANKCODE = 'n'; // example: AT57 20111 40014400144
    static Long n(String bankCode, Object ignored) {
    	// branchC wird ignoriert
    	return Long.parseLong(bankCode, 10);
    }
    
    static final Character SORTCODE_LIKE = 's'; // example: BG64 UNCR 96601 010688021
    // ignore alpha bankCode, numeric branchCode, sort code Bsp GB IE 
    static Long s(String ignore, Object branchCode) {
    	// ignore alpha bankCode
    	return Long.parseLong(branchCode.toString(), 10);
    }
    
    // TODO commands.put('0', () -> System.out.println("numeric bankCode, nil branch, Bsp LU"
    static Long idFunc0(String bankC, Object branchC) {
    	return 999L; // TODO
    }
    

	/*
	
	Interface BiFunction<T,U,R>
	
	    Type Parameters:
	        T - the type of the first argument to the function
	        U - the type of the second argument to the function
	        R - the type of the result of the function 
	        
	Bsp.: Long id = a.apply("ABNA", null);
	
	 */
	static Map<Character, BiFunction<String, Object, Long>> idFunc = new HashMap<>();

    static {  	
    	idFunc.put(ALPHA_BANKCODE,                  (bankC, branchC) -> a(bankC, branchC));
    	idFunc.put(BANKCODE_AND_BRANCHCODE_NUMERIC, (bankC, branchC) -> b(bankC, branchC));
    	idFunc.put(NUMERIC_BANKCODE,                (bankC, branchC) -> n(bankC, branchC));
    	idFunc.put(SORTCODE_LIKE,                   (bankC, branchC) -> s(bankC, branchC));
    	idFunc.put(BANKCODE_WITH_ZERO_BRANCHCODE,   (bankC, branchC) -> z(bankC, branchC));  	
    }
}
