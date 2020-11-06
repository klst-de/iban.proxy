package com.klst.ibanTest;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import com.klst.iban.InternationalBankAccountNumber;

public class IBANTest {

    private static final String VALID_IBAN = "NL91ABNA0417164300"; // alpha bankCode property 
                                           // FI5542345670000081    numeric bankCode property
                                           // BG80BNBG96611020345678  alpha bankCode property with branchCode  
                                           // AD1200012030200359100100 numeric bankCode + branchCode property
                                           // IT60X0542811101000000123456 Format: Kontrollzeichen+BankCode+BranchCode+account
                                           // GR1601101250000000012300695 bankCode + branchCode mit führender Null
    
    private static final String INVALID_IBAN = "NL12ABNA0417164300";

    @Test
    public void getCountryCodeForInvalidIBAN() {
    	InternationalBankAccountNumber iban = new InternationalBankAccountNumber(INVALID_IBAN);
        assertNull(iban.getCountryCode());
    }

    @Test
    public void getCountryCodeForValidIBAN() {
    	InternationalBankAccountNumber iban = new InternationalBankAccountNumber(VALID_IBAN);
        assertEquals("NL", iban.getCountryCode());
        assertEquals("91", iban.getCheckDigits());
        assertEquals("ABNA0417164300", iban.getBbanPart());
        assertEquals("ABNA", iban.getBankData().getBankIdentifier());
        assertEquals(0, iban.getBankData().getBankCode());
        assertNull(iban.getBankData().getBranchCode());
        assertEquals("ABNA", iban.getBankDataPart());
        
        iban = new InternationalBankAccountNumber("FI5542345670000081");
        assertEquals("FI", iban.getCountryCode());
        assertEquals("55", iban.getCheckDigits());
        assertEquals("42345670000081", iban.getBbanPart());
        assertEquals("423", iban.getBankData().getBankIdentifier());
        assertEquals(423, iban.getBankData().getBankCode());
        assertNull(iban.getBankData().getBranchCode());
        assertEquals("423", iban.getBankDataPart());
        
        iban = new InternationalBankAccountNumber(" BG80BNBG96611020345678"); // trimmed leading spaces
        assertEquals("BG", iban.getCountryCode());
        assertEquals("80", iban.getCheckDigits());
        assertEquals("BNBG96611020345678", iban.getBbanPart());
        assertEquals("BNBG", iban.getBankData().getBankIdentifier());
        assertEquals(0, iban.getBankData().getBankCode());
        assertEquals("9661", iban.getBankData().getBranchCode());
        assertEquals("BNBG9661", iban.getBankDataPart());
        
        iban = new InternationalBankAccountNumber("AD1200012030200359100100 "); // trimmed ending spaces
        assertEquals("AD", iban.getCountryCode());
        assertEquals("12", iban.getCheckDigits());
        assertEquals("00012030200359100100", iban.getBbanPart());
        assertEquals("0001", iban.getBankData().getBankIdentifier());
        assertEquals(1, iban.getBankData().getBankCode());
        assertEquals("2030", iban.getBankData().getBranchCode());
        assertEquals("00012030", iban.getBankDataPart());
        
        iban = new InternationalBankAccountNumber("IT60X0542811101000000123456");
        assertEquals("IT", iban.getCountryCode());
        assertEquals("60", iban.getCheckDigits());
        assertEquals("X0542811101000000123456", iban.getBbanPart());
        assertEquals("05428", iban.getBankData().getBankIdentifier());
        assertEquals(5428, iban.getBankData().getBankCode());
        assertEquals("11101", iban.getBankData().getBranchCode());
        assertEquals("0542811101", iban.getBankDataPart());
              
        iban = new InternationalBankAccountNumber("GR1601101250000000012300695");
        assertEquals("GR", iban.getCountryCode());
        assertEquals("16", iban.getCheckDigits());
        assertEquals("01101250000000012300695", iban.getBbanPart());
        assertEquals("011", iban.getBankData().getBankIdentifier());
        assertEquals(11, iban.getBankData().getBankCode());
        assertEquals("0125", iban.getBankData().getBranchCode());
        assertEquals("0110125", iban.getBankDataPart());
        
		iban = new InternationalBankAccountNumber("DE79701500000111153375");
		assertEquals("DE", iban.getCountryCode());
		assertEquals("79", iban.getCheckDigits());
		assertEquals("701500000111153375", iban.getBbanPart());
		assertEquals("70150000", iban.getBankData().getBankIdentifier());
		assertEquals(70150000, iban.getBankData().getBankCode());
		assertNull(iban.getBankData().getBranchCode());
		assertEquals("70150000", iban.getBankDataPart());
        
   }

}
