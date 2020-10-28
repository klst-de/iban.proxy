package com.klst.ibanTest;

//import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Test;

import com.klst.iban.BankId;
import com.klst.iban.IbanToBankProxy;
import com.klst.iban.Result.BankData;

public class ProxyTest {

	private static Logger LOG;
	private static final LogManager logManager = LogManager.getLogManager(); // Singleton
	
	private static final String API_KEY = API_Key_Provider.API_KEY;
	
    @BeforeClass
	public static void staticSetup() {
    	URL url = ProxyTest.class.getClassLoader().getResource("testLogging.properties");
		try {
			File file = new File(url.toURI());
			logManager.readConfiguration(new FileInputStream(file));
		} catch (IOException | URISyntaxException e) {
			LOG = Logger.getLogger(ProxyTest.class.getName());
			LOG.warning(e.getMessage());
		}
		LOG = Logger.getLogger(ProxyTest.class.getName());
	}

	// https://www.afa.ad/ca/entitats-supervisades/entitats-financeres/entitats-bancaries/registre-dentitats-autoritzades :
	// andbanc Número de registre: EB 01/95 ==> AD12 0001 2030 200359100100 
	//  Banca Privada d'Andorra, SA : Número de registre: EB 04/95 ==>? AD?? 0004 ???? 200359100100
	// Denominació social: BancSabadell d'Andorra, SA : Número de registre: EB 08/99
	// Denominació social: Crèdit Andorrà, SA : Número de registre: EB 02/95 =======> nein ist 0003
	// Denominació social: Mora Banc Grup, SA : Número de registre: EB 06/95 =======> nein ist 0004
	// Denominació social: Vall Banc, SA : Número de registre: EB 09/15
	// siehe http://www.hiarribarem.com/
	// AD45 0001 0000 4112 2010 0100
	// AD87 0003 1101 1179 1941 0101
	// AD86 0004 0019 0001 4014 5012
    private static final String VALID_AD_IBAN = "AD8600040019000140145012"; // num bankCode + branchCode, id = 40019
    // BG ibans see http://en-m.redcross.bg/help_us/how_to_help/bank_donation
    private static final String VALID_BG_IBAN = "BG64UNCR96601010688021"; // alpha bankCode property + branchCode 
    private static final String VALID_NL_IBAN = "NL91ABNA0417164300"; // alpha bankCode property 
    
    @Test
    public void bankIdTest() {
		try {
			Long id = BankId.getBankId("AT", "20111", null); // AT57 20111 40014400144
			assertEquals(20111, BankId.getBankId("AT", "20111", null));
			assertEquals(9661, BankId.getBankId("BG", "BNBG", "9661")); // BG80 BNBG 9661 1020345678
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
//    @Test
    public void withApiKey() {
    	IbanToBankProxy proxy = new IbanToBankProxy(API_KEY);
    	BankData bankData = proxy.getBankData(VALID_NL_IBAN);
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());
    	assertEquals("ABNA", bankData.getBankIdentifier()); // "id":   8814
    	assertEquals(15, bankData.getBankSupports());

    	bankData = proxy.getBankData(VALID_AD_IBAN);
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

//    	bankData = proxy.getBankData(VALID_BG_IBAN);
//    	assertNotNull(bankData);
//    	LOG.info(bankData.toString());
//    	assertEquals("UNCR", bankData.getBankIdentifier());
//INFORMATION: [CountryIso:null, Bic:UNCRBGSFXXX, BankCode:UNCR, BranchCode:9660, Branch:"", Name:"UNICREDIT BULBANK AD", Address:"7 SVETA NEDELYA SQUARE", BankSupports:9, Zip:"1000", City:" SOFIA"]
//
    	
//    	bankData = proxy.getBankData("LB73005600000000010620130001"); //VALID_LB_IBAN
//INFORMATION: [Bic:AUDBLBBXXXX, BankCode:56, BranchCode:, Name:"Banque Audi S.A.L. - Audi Saradar Group", Address:"Omar Daouk Street,Bab Idriss 2021 8102 BEIRUT LEBANON", Zip:"", City:""] 
    	bankData = proxy.getBankData("LB19005600000000010620130003"); //gleiche Bank
//INFORMATION: [Bic:AUDBLBBXXXX, BankCode:56, BranchCode:, Name:"Banque Audi S.A.L. - Audi Saradar Group", Address:"Omar Daouk Street,Bab Idriss 2021 8102 BEIRUT LEBANON", Zip:"", City:""] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("QA37BRWA000000000200000094340");
//INFORMATION: [Bic:BRWAQAQAXXX, BankCode:BRWA, BranchCode:, Name:"BARWA BANK P.Q.S.C.", Address:"Barwa Al Sadd Towers, Tower No. 1 Grand Hamad Street 27778 DOHA QATAR", Zip:"", City:""] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("SE7195000099604206029896");
//INFORMATION: [Bic:NDEASESSXXX, BankCode:950, BranchCode:, Name:"Nordea", Address:"Smålandsgatan 17", BankSupports:1, Zip:"105 71", City:"Stockholm"] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("IT49H0306909606100000000060");
//INFORMATION[Bic:BCITITMMXXX, BankCode:3069, BranchCode:09606, Branch:"FILIALE ACCENTRATA TERZO SETTORE", Name:"INTESA SANPAOLO SPA", Address:"PIAZZA PAOLO FERRARI, 10", BankSupports:15, Zip:"20121", City:"MILANO"] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("CZ4606000000000223832410");
//INFORMATION: [Bic:AGBACZPPXXX, BankCode:600, BranchCode:000000, Name:"MONETA Money Bank, a.s.", Address:"BB CENTRUM VYSKOCILOVA 1422/1A", BankSupports:1, Zip:"14028", City:"PRAGUE 4"] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("AE890230000001000551554");
//INFORMATION: [Bic:CBDUAEADXXX, BankCode:23, BranchCode:, Name:"Commercial Bank of Dubai", Address:"AL ITTIHAD STREET", Zip:"", City:"DUBAI"] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("GB88BOFS12248106741391"); // BOFSGBS1BBL
//INFORMATION: [Bic:BOFSGBS1BBL, BankCode:BOFS, BranchCode:122481, Branch:"THE DIRECT BUSINESS BANK 1", Name:"Bank of Scotland plc", Address:"Teviot House South Gyle Crescent ", BankSupports:1, Zip:"EH12 9DR", City:"Edinburgh"] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("SK8375000000004011203856");
//INFORMATION: [Bic:CEKOSKBXXXX, BankCode:7500, BranchCode:000000, Name:"Ceskoslovenská obchodná banka, a.s", Address:"Michalská 18", BankSupports:15, Zip:"815 63", City:"Bratislava"] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    	bankData = proxy.getBankData("GR5202600300000910200744514");
//INFORMATION: [Bic:ERBKGRAAXXX, BankCode:26, BranchCode:0030, Branch:"KAROLOU DIL", Name:"EUROBANK ERGASIAS S.A.", Address:"13, KAROLOU DIL STR.", BankSupports:15, Zip:"546 23", City:"THESSALONIKI"] 
    	assertNotNull(bankData);
    	LOG.info(bankData.toString());

    }
    
    @Test
    public void noApiKey() {
    	IbanToBankProxy proxy = new IbanToBankProxy();
    	
    	BankData bankData = proxy.getBankData(VALID_AD_IBAN);
    	assertNotNull(bankData);
    	assertEquals(1, bankData.getBankSupports());
    	
//    	BankData bankData = proxy.getBankData("AT572011140014400144");
//    	assertNotNull(bankData);
//    	LOG.info(bankData.toString());
//    	assertEquals(20111, bankData.getBankCode()); // "id":   20111
//    	assertEquals(15, bankData.getBankSupports());
////    	assertNull(proxy.getBankData("AT611904300234573201"));
    	
    	bankData = proxy.getBankData(VALID_NL_IBAN); // warn
    	assertNotNull(bankData);
    	assertEquals("ABNA", bankData.getBankIdentifier()); // "id":   8814
    	assertEquals(15, bankData.getBankSupports());
    }

    // BG80BNBG96611020345678 => "support_codes":1

}
