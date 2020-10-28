package com.klst.ibanTest;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import com.klst.iban.BusinessIdentifierCode;

public class BICTest {

	private static Logger LOG;
	private static final LogManager logManager = LogManager.getLogManager(); // Singleton
	
	static {
    	URL url = BICTest.class.getClassLoader().getResource("testLogging.properties");
		try {
			File file = new File(url.toURI());
			logManager.readConfiguration(new FileInputStream(file));
		} catch (IOException | URISyntaxException e) {
			LOG = Logger.getLogger(BICTest.class.getName());
			LOG.warning(e.getMessage());
		}
		LOG = Logger.getLogger(BICTest.class.getName());
	}
	
    @BeforeClass
	public static void staticSetup() {
		LOG.info("staticSetup fertig."); // nicht angezeigt in eclipse ?
    }

	@Test
	public void validBicTest() {
		BusinessIdentifierCode bic = new BusinessIdentifierCode("ACABAZ22");
		assertEquals(true, bic.isValid());
	}
	
    @Test
    public void testIllegalArgumentExceptionInInstitutionCode() {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> BusinessIdentifierCode.bankCodeToId(null));
        assertEquals("bankCode is null.", exception.getMessage());
        
    	exception = assertThrows(IllegalArgumentException.class, () -> BusinessIdentifierCode.bankCodeToId("AB"));
        assertEquals("'AB'.length NOT 4.", exception.getMessage());
    }
    
	@Test
	public void bankCodeToIdTest() {
		LOG.info("start");
		assertEquals(0, BusinessIdentifierCode.bankCodeToId("AAAA"));
		assertEquals(1, BusinessIdentifierCode.bankCodeToId("BAAA"));
		assertEquals(2, BusinessIdentifierCode.bankCodeToId("CAAA"));
		assertEquals(3, BusinessIdentifierCode.bankCodeToId("DAAA"));
		assertEquals(4, BusinessIdentifierCode.bankCodeToId("EAAA"));
		assertEquals(5, BusinessIdentifierCode.bankCodeToId("FAAA"));
		assertEquals(6, BusinessIdentifierCode.bankCodeToId("GAAA"));
		assertEquals(7, BusinessIdentifierCode.bankCodeToId("HAAA"));
		assertEquals(8, BusinessIdentifierCode.bankCodeToId("IAAA"));
		assertEquals(9, BusinessIdentifierCode.bankCodeToId("JAAA"));
		assertEquals(10, BusinessIdentifierCode.bankCodeToId("KAAA"));
		assertEquals(11, BusinessIdentifierCode.bankCodeToId("LAAA"));
		assertEquals(12, BusinessIdentifierCode.bankCodeToId("MAAA"));
		assertEquals(13, BusinessIdentifierCode.bankCodeToId("NAAA"));
		assertEquals(14, BusinessIdentifierCode.bankCodeToId("OAAA"));
		assertEquals(15, BusinessIdentifierCode.bankCodeToId("PAAA"));
		assertEquals(16, BusinessIdentifierCode.bankCodeToId("QAAA"));
		// ...
		assertEquals(25, BusinessIdentifierCode.bankCodeToId("ZAAA"));
		assertEquals(26, BusinessIdentifierCode.bankCodeToId("ABAA"));
	}

}
