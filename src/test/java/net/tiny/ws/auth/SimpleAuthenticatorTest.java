package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class SimpleAuthenticatorTest {

	@Test
	public void testCheckCredentials() throws Exception {
		SimpleAuthenticator auth = new SimpleAuthenticator();
		assertEquals("simple", auth.getRealm());

		auth.setEncode(false);
		auth.setUsername("Hoge");
		auth.setPassword("password");
		assertTrue(auth.checkCredentials("Hoge", "password"));
		assertFalse(auth.checkCredentials("Fuga", "password"));
		assertFalse(auth.checkCredentials("Hoge", "guess"));

		auth.setPassword("jZUgQ8BHZOPJhFCHve0kyg==");
		auth.setEncode(true);
		assertTrue(auth.checkCredentials("Hoge", "password"));
		assertFalse(auth.checkCredentials("Fuga", "password"));
		assertFalse(auth.checkCredentials("Hoge", "guess"));
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
		SimpleAuthenticator auth = new SimpleAuthenticator();

		auth.setUsername("Hoge");
		auth.setPassword("jZUgQ8BHZOPJhFCHve0kyg==");
		assertTrue(auth.checkCredentials("Hoge", "password"));
		assertFalse(auth.checkCredentials("Hoge", "guess"));

		auth.setEncode(false);
		assertTrue(auth.checkCredentials("Hoge", "jZUgQ8BHZOPJhFCHve0kyg=="));
		assertFalse(auth.checkCredentials("Hoge", "password"));
		assertFalse(auth.checkCredentials("Hoge", "guess"));
	}

	@Test
	public void testInvaildPassword() throws Exception {
		SimpleAuthenticator auth = new SimpleAuthenticator();
		//Default encode is true
		auth.setUsername("Hoge");
		auth.setPassword("password");
		assertFalse(auth.checkCredentials("Hoge", "password"));
		assertFalse(auth.checkCredentials("Hoge", "guess"));

		auth.setEncode(false);
		assertTrue(auth.checkCredentials("Hoge", "password"));

		auth.setEncode(true);
		assertFalse(auth.checkCredentials("Hoge", "password"));

		auth.setPassword("jZUgQ8BHZOPJhFCHve0kyg==");
		assertTrue(auth.checkCredentials("Hoge", "password"));
	}
}
