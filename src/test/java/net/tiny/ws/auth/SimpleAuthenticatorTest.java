package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class SimpleAuthenticatorTest {

    @Test
    public void testPlaintextsCredentials() throws Exception {
        SimpleAuthenticator auth = new SimpleAuthenticator();
        assertEquals("simple", auth.getRealm());

        auth.setEncode(false);
        auth.setUsername("Hoge");
        auth.setPassword("password");
        assertTrue(auth.checkCredentials("Hoge", "password"));
        assertFalse(auth.checkCredentials("Fuga", "password"));
        assertFalse(auth.checkCredentials("Hoge", "guess"));
    }

    @Test
    public void testCryptedCredentials() throws Exception {
        String encoded = Crypt.create("DES").encrypt("password");
        System.out.println(encoded);
        String pass = Crypt.create("DES").decrypt(encoded);
        assertEquals("password", pass);

        SimpleAuthenticator auth = new SimpleAuthenticator();
        assertEquals("simple", auth.getRealm());
        auth.setUsername("Hoge");
        auth.setEncode(true);
        auth.setPassword(encoded);

        assertFalse(auth.checkCredentials("Fuga", "password"));
        assertFalse(auth.checkCredentials("Hoge", "guess"));

        assertTrue(auth.checkCredentials("Hoge", "password"));
    }

    @Test
    public void testNullCredentials() throws Exception {
        SimpleAuthenticator auth = new SimpleAuthenticator();
        // Not setting username , password
        assertFalse(auth.checkCredentials("Hoge", "password"));
        assertFalse(auth.checkCredentials("", ""));
        assertFalse(auth.checkCredentials(null, null));
    }

    @Test
    public void testVaildPassword() throws Exception {
        String encoded = Crypt.create("DES").encrypt("password");
        System.out.println(encoded);
        SimpleAuthenticator auth = new SimpleAuthenticator();
        assertEquals("simple", auth.getRealm());
        auth.setEncode(true);
        auth.setUsername("Hoge");
        auth.setPassword(encoded);
        assertTrue(auth.checkCredentials("Hoge", "password"));
        assertFalse(auth.checkCredentials("Hoge", "guess"));

        auth.setEncode(false);
        assertTrue(auth.checkCredentials("Hoge", encoded));
        assertFalse(auth.checkCredentials("Hoge", "password"));
        assertFalse(auth.checkCredentials("Hoge", "guess"));
    }

}
