package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class SecureKeyTest {

    @Test
    public void testOneProcess() throws Exception {
    	// Server encrypt, Client decrypt
    	SecureKey secureKey = new SecureKey("DES", 4);
    	Map<String, String> publicKeys = secureKey.createPublicKeys();
    	assertEquals(3, publicKeys.size());
    	String modulus = publicKeys.get("modulus");
    	String exponent = publicKeys.get("exponent");
    	String publicKey = publicKeys.get("publicKey");

    	String password = "password";
    	// Server site by private key
    	String enPassword = secureKey.encrypt(password);
    	System.out.println(enPassword);
    	// Client site by public key
    	String decryptedData = Crypt.decryptByPublicKey(publicKey, enPassword);
		System.out.println(decryptedData);
		assertEquals(password, decryptedData);

		// Client encrypt, Server decrypt
		secureKey = new SecureKey("DES", 4);
    	publicKeys = secureKey.createPublicKeys();
    	modulus = publicKeys.get("modulus");
    	exponent = publicKeys.get("exponent");
    	publicKey = publicKeys.get("publicKey");

    	// Client site by public key
    	String encryptedData = Crypt.encryptByPublicKey(modulus, exponent, password);
		System.out.println(encryptedData);
		// Server site by private key
		String decrypted = secureKey.decrypt(encryptedData);
    	System.out.println(decrypted);
    	assertEquals(password, decrypted);
    }

    @Test
    public void testAfterCreatePublicKeys() throws Exception {
    	String password = "password";

    	// Server encrypt
    	SecureKey secureKey = new SecureKey("DES", 4);
    	Map<String, String> publicKeys = secureKey.createPublicKeys();
    	assertEquals(3, publicKeys.size());
    	String modulus = publicKeys.get("modulus");
    	String exponent = publicKeys.get("exponent");
    	//String publicKey = publicKeys.get("publicKey");

    	// Client site by public key
    	String encryptedData = Crypt.encryptByPublicKey(modulus, exponent, password);
		System.out.println(encryptedData);

		try {
			publicKeys = secureKey.createPublicKeys();
	    	// Server site decrypt
			secureKey.decrypt(encryptedData);
			fail();
		} catch (Exception ex) {
			assertTrue(ex instanceof SecurityException);
			assertEquals("Decryption error", ex.getMessage());
		}
    }

    @Test
    public void testCreateCacheMap() throws Exception {
    	Map<String, String> map = SecureKey.createCacheMap(4);
    	map.put("1", "v1");
    	map.put("last", "1");
    	map.put("2", "v2");
    	map.put("last", "2");
    	map.put("3", "v3");
    	map.put("last", "3");
    	assertEquals(4, map.size());
    	map.put("4", "v4");
    	map.put("last", "4");
    	map.put("5", "v5");
    	map.put("last", "5");
    	assertEquals(4, map.size());
    	assertEquals("{3=v3, 4=v4, 5=v5, last=5}", map.toString());
    	System.out.println(map.toString());
    	map.put("6", "v6");
    	map.put("last", "6");
    	assertEquals(4, map.size());
    	assertEquals("{4=v4, 5=v5, 6=v6, last=6}", map.toString());
    	System.out.println(map.toString());
    }

}
