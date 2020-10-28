package com.klst.iban;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.logging.Logger;

/*

Es gibt keine unique BankId. Weder weltweit, noch für ein Land (CountryCode).

Dies is der Versuch aus properties bankCode, branchCode eine Id zu berechnen.
Pro CountryCode gibt es ein anderes Verfahren. Also gibt es mehrere idFunctions:

- a : alpha bankCode, nil branch, Bsp NL
- b : numeric bankCode, numeric branchCode, Bsp GR (nicht AD, das liefert: bankCode+0000 da jeder branchCode gültig ist)
...

 */
// aus Java8BiFunction1 https://mkyong.com/java8/java-8-bifunction-examples/
public class BankId {

	private static final Logger LOG = Logger.getLogger(BankId.class.getName());
	
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
    	Character cmd = countryToFunc.get(countryCode);
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
    

	static Map<String, Character> countryToFunc = new HashMap<>();
	static Map<Character, BiFunction<String, Object, Long>> idFunc = new HashMap<>();

//    public static void main(String[] args) {
//
//        // takes two Integers and return an Integer
//        BiFunction<Integer, Integer, Integer> func = (x1, x2) -> x1 + x2;
//
//        Integer result = func.apply(2, 3);
//
//        System.out.println(result); // 5
//
//        // take two Integers and return an Double
//        BiFunction<Integer, Integer, Double> func2 = (x1, x2) -> Math.pow(x1, x2);
//
//        Double result2 = func2.apply(2, 4);
//
//        System.out.println(result2);    // 16.0
//
//        // take two Integers and return a List<Integer>
//        BiFunction<Integer, Integer, List<Integer>> func3 = (x1, x2) -> Arrays.asList(x1 + x2);
//
//        List<Integer> result3 = func3.apply(2, 3);
//
//        System.out.println(result3);
//
///*
//
//Interface BiFunction<T,U,R>
//
//    Type Parameters:
//        T - the type of the first argument to the function
//        U - the type of the second argument to the function
//        R - the type of the result of the function 
//
// */
//
//        // meine:
//        BiFunction<String, String, Long> a = (bankC, branchC) -> a(bankC, branchC);
//        BiFunction<String, String, Long> b = (bankC, branchC) -> b(bankC, branchC);
//
//        Long id = a.apply("ABNA", null);
//        System.out.println(id);    // 16.0
//
//    }

    static {
    	countryToFunc.put("AD", BANKCODE_WITH_ZERO_BRANCHCODE);
    	countryToFunc.put("AT", NUMERIC_BANKCODE);
    	countryToFunc.put("BE", NUMERIC_BANKCODE);
    	countryToFunc.put("BG", SORTCODE_LIKE);
    	countryToFunc.put("GR", BANKCODE_AND_BRANCHCODE_NUMERIC);
    	countryToFunc.put("NL", ALPHA_BANKCODE);
    	
    	idFunc.put(ALPHA_BANKCODE,                  (bankC, branchC) -> a(bankC, branchC));
    	idFunc.put(BANKCODE_AND_BRANCHCODE_NUMERIC, (bankC, branchC) -> b(bankC, branchC));
    	idFunc.put(NUMERIC_BANKCODE,                (bankC, branchC) -> n(bankC, branchC));
    	idFunc.put(SORTCODE_LIKE,                   (bankC, branchC) -> s(bankC, branchC));
    	idFunc.put(BANKCODE_WITH_ZERO_BRANCHCODE,   (bankC, branchC) -> z(bankC, branchC));  	
    }
}
