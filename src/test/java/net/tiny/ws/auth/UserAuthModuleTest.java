package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class UserAuthModuleTest {

    @Test
	public void testLoginCallback() throws Exception {
		String config = "src/test/resources/jaas/jaas.conf";
		System.setProperty("java.security.auth.login.config", config);

		//Input user-name and password, remote address
		CallbackContext callbackContext = new CallbackContext()
				.setUsername("admin")
				.setPassword("password")
				.setAddress("127.0.0.1");

		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, new DummyAccountService());

		final CallbackHandler callbackHandler = callbackContext.getCallbackHandler();
		assertNotNull(callbackHandler);
		assertTrue(callbackHandler instanceof DefaultCallbackHandler);
		System.out.println("Callback: " + callbackHandler.toString());
		String module = "test";

		Subject subject = null;
		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(module, callbackHandler);
			// Perform authentication
		    context.login();

		    // After login
		    subject = context.getSubject();
		    assertNotNull(subject);
		    Set<Principal> principals = subject.getPrincipals();
		    assertFalse(principals.isEmpty());
		    assertEquals(4, principals.size());
		    Principal up = principals.iterator().next();
		    assertTrue(up instanceof UserPrincipal);

		    Set<UserPrincipal> ups = subject.getPrincipals(UserPrincipal.class);
		    assertFalse(ups.isEmpty());
		    assertTrue(ups.contains(new UserPrincipal("admin")));

		    Set<RolePrincipal> roles = subject.getPrincipals(RolePrincipal.class);
		    assertFalse(roles.isEmpty());
		    assertEquals(3, roles.size());
		    assertTrue(roles.contains(new RolePrincipal("admin:product")));
		    assertTrue(roles.contains(new RolePrincipal("admin:productCategory")));
		    assertTrue(roles.contains(new RolePrincipal("admin:productNotify")));
		    Set<Object> publicPrincipals = subject.getPublicCredentials();
		    assertTrue(publicPrincipals.isEmpty());

		    Set<Object> privatePrincipals = subject.getPrivateCredentials();
		    assertFalse(privatePrincipals.isEmpty());
		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Login failed. " + ex.getMessage());
		}

		try {
			assertNotNull(subject);
			LoginContext context = new LoginContext(module, subject);
		    context.logout();

		    //After logout
		    subject = context.getSubject();
		    assertNotNull(subject);
		    assertTrue(subject.getPrincipals().isEmpty());
		    assertTrue(subject.getPrivateCredentials().isEmpty());
		    assertTrue(subject.getPublicCredentials().isEmpty());

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Logout failed. " + ex.getMessage());
		}
	}


	/*******************************/
	class DummyAccountService implements AuthenticationService {

		@Override
		public Set<String> getRoles(String username) {
			Set<String> rolesSet = new HashSet<>();
			rolesSet.add("admin:product");
			rolesSet.add("admin:productCategory");
			rolesSet.add("admin:productNotify");
			return rolesSet;
		}

		@Override
		public void verify(String username, String password, String address, LocalDateTime time) throws LoginException {
			if(!(username.equals("admin") || username.equals("Jone") || username.equals("Wayne"))) {
				throw new AccountNotFoundException(username);
			}
			if(username.equals("Jone")) {
				throw new AccountLockedException(username);
			}
			if(username.equals("Wayne")) {
				throw new AccountExpiredException(username);
			}
			if(!password.equals("password")) {
				throw new FailedLoginException(username);
			}
		}
	}
}
