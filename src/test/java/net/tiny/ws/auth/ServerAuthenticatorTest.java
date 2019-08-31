package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ServerAuthenticatorTest {

	@BeforeAll
	public static void beforeAll() throws Exception {
		LogManager.getLogManager().readConfiguration(
				Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
	}

	@Test
	public void testCheckCredentials() throws Exception {
		String config = "src/test/resources/jaas/jaas.conf";
		System.setProperty("java.security.auth.login.config", config);

		ServerAuthenticator auth = new ServerAuthenticator();
		assertEquals("default", auth.getRealm());

		AccountService service = new AccountService();
		service.setPath("src/test/resources/jaas/passwd");
		auth.setService(service);

		assertTrue(auth.checkCredentials("admin", "password"));
		assertTrue(auth.checkCredentials("Hoge", "password1"));
		assertTrue(auth.checkCredentials("Fuga", "password2"));

		assertFalse(auth.checkCredentials("Wayne", "password0")); //AccountExpiredException
		assertFalse(auth.checkCredentials("Jone", "password")); //AccountLockedException

		assertFalse(auth.checkCredentials("admin", "guess"));
		assertFalse(auth.checkCredentials("Hoge", "password"));
		assertFalse(auth.checkCredentials("Fuga", "password"));
	}
}
