package com.klst.iban;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

import com.github.miachm.sods.Range;
import com.github.miachm.sods.Sheet;
import com.klst.iban.datastore.LocalFileProxy;
import com.klst.ods.Ods;

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
	Map<String, Integer> deBLZtoID = null;
	
	private BankId() {
    	// sepa countries
    	countryToFunc.put("AD", BANKCODE_WITH_ZERO_BRANCHCODE);
    	countryToFunc.put("AT", NUMERIC_BANKCODE);
    	countryToFunc.put("BE", NUMERIC_BANKCODE);
    	countryToFunc.put("BG", SORTCODE_LIKE);
    	countryToFunc.put("CH", NUMERIC_BANKCODE);
    	countryToFunc.put("CY", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("CZ", NUMERIC_BANKCODE);
    	countryToFunc.put("DE", NUMERIC_BANKCODE_WITH_MAP);
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
    	// non sepa countries
    	countryToFunc.put("ME", NUMERIC_BANKCODE);
    	countryToFunc.put("MK", NUMERIC_BANKCODE);
    	countryToFunc.put("SA", NUMERIC_BANKCODE);
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
    
    static final Character NUMERIC_BANKCODE_WITH_MAP = 'm'; // example: DE89 37040044  0532013000
    static Long m(String bankCode, Object ignored) {
    	BankId bankId = getInstance();
    	Integer id = bankId.getDeBLZtoID().get(bankCode);
    	return id==null ? null : (long) id;
    }
    
    public Map<String, Integer> getDeBLZtoID() {
    	if(deBLZtoID==null) {
    		loadDEmap();
    	}
    	return deBLZtoID;
    }
    static final String DE_ODS_RESOURCE = LocalFileProxy.RESOURCE_DATA_PATH + "doc/DE/blzToId.ods";
    // "routingno";"count";"id";"updated";"swiftcode";"name"
	static final int COL_BLZ                     =  0;  // aka sortCode aka bankCode
	static final int COL_COUNTSTAR               =  1;
	static final int COL_ID                      =  2;
	static final int COL_UPDATED                 =  3;
	static final int COL_BIC                     =  4;
	static final int COL_Bank                    =  5;  // name
	static final int NUMCOLUMNS                  =  6;

    private void loadDEmap() {
    	deBLZtoID = new HashMap<>();
//    	deBLZtoID.put("37020500", 531);
//    	37020500	1	531	2020-09-07 00:00:00	BFSWDE33XXX	Bank für Sozialwirtschaft , Köln

        List<Sheet> sheets = Ods.getSheets(DE_ODS_RESOURCE);
        
    	int numColumns = NUMCOLUMNS;

        Map<String,Integer> nonEmptySheets = new Hashtable<String,Integer>();
        Sheet nonEmptySheet = Ods.getNonEmptySheet(sheets, nonEmptySheets, numColumns);
        LOG.info("file "+DE_ODS_RESOURCE+" has nonEmptySheets/sheets:"+nonEmptySheets.size()+"/"+sheets.size());
        if(nonEmptySheets.size()==1) {
        	Collection<Integer> collection = nonEmptySheets.values();
        	int numRows = collection.iterator().next();
        	Range range = nonEmptySheet.getRange(0, 0, numRows, numColumns);
        	LOG.info("range.getNumRows()="+range.getNumRows() + " range.getNumColumns()="+range.getNumColumns());
    		Object[][] values = range.getValues();
    		// r==0 ist colname, daher start bei 1
    		for (int r = 1; r < range.getNumRows(); r++) {
    			Object blz = values[r][COL_BLZ]; // Double und nicht String
    			Object id = values[r][COL_ID]; // Double und nicht Integer
    			String sBlz = String.format("%08d", (int)(double)((Double)blz));
    			int i = (int)(double)((Double)id);
//    			for (int c = 0; c < range.getNumColumns(); c++) {
//    				Object v = values[r][c];
//            		Object cellObect = range.getCell(r, c).getValue();
//    				LOG.info("r("+r+"),c:"+c + " " + (v==null? "null" : v.getClass()) + " " + cellObect);
//    			}
    			LOG.finer("r("+r+"):"+blz+" "+id + " ==> " + sBlz + ":"+i);
    			deBLZtoID.put(sBlz, i);
    		}
        }
    }

    static final Character SORTCODE_LIKE = 's'; // example: BG64 UNCR 96601 010688021
    // ignore alpha bankCode, numeric branchCode, sort code Bsp GB IE 
    static Long s(String ignore, Object branchCode) {
    	// ignore alpha bankCode
    	return Long.parseLong(branchCode.toString(), 10);
    }
    
    // TODO commands.put('0', () -> System.out.println("numeric bankCode, nil branch, Bsp LU"
    static Long idFunc0(String bankC, Object branchC) {
    	LOG.warning("TODO TODO TODO TODO TODO TODO "); //TODO 
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
    	idFunc.put(NUMERIC_BANKCODE_WITH_MAP,       (bankC, branchC) -> m(bankC, branchC));
    	idFunc.put(SORTCODE_LIKE,                   (bankC, branchC) -> s(bankC, branchC));
    	idFunc.put(BANKCODE_WITH_ZERO_BRANCHCODE,   (bankC, branchC) -> z(bankC, branchC));  	
    }
}
