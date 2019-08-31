package net.tiny.ws.auth;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;


public class AuthenticateTest {

    @Test
	public void testLoginFailed() throws Exception {
    	final AccountService authService = new AccountService();
    	authService.setPath("src/test/resources/jaas/passwd");

		String config = "src/test/resources/jaas/jaas.conf";
		String alias = "embed";
		System.setProperty("java.security.auth.login.config", config);

		CallbackContext callbackContext = new CallbackContext()
			.setUsername("tom1")
			.setPassword("password")
			.setAddress("127.0.0.1");
		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, authService);
		CallbackHandler callbackHandler = callbackContext.getCallbackHandler();
		assertNotNull(callbackHandler);

		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(alias, callbackHandler);
			// Perform authentication
		    context.login();

		    fail("tom1 not found.");
		} catch (LoginException ex) {
			assertTrue(ex instanceof AccountNotFoundException);
		}

		callbackContext = new CallbackContext()
				.setUsername("admin")
				.setPassword("passxxxx")
				.setAddress("127.0.0.1");
		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, authService);
		callbackHandler = callbackContext.getCallbackHandler();
		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(alias, callbackHandler);
			// Perform authentication
		    context.login();
		    fail("admin password is wrong.");
		} catch (LoginException ex) {
			ex.printStackTrace();
			assertTrue(ex instanceof FailedLoginException);
		}

		callbackContext = new CallbackContext()
				.setUsername("Jone")
				.setPassword("password")
				.setAddress("127.0.0.1");
		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, authService);
		callbackHandler = callbackContext.getCallbackHandler();
		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(alias, callbackHandler);
			// Perform authentication
		    context.login();
		    fail("Jone is locked.");
		} catch (LoginException ex) {
			assertTrue(ex instanceof AccountLockedException);
		}

		callbackContext = new CallbackContext()
				.setUsername("Wayne")
				.setPassword("password")
				.setAddress("127.0.0.1");
		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, authService);
		callbackHandler = callbackContext.getCallbackHandler();
		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(alias, callbackHandler);
			// Perform authentication
		    context.login();
		    fail("Wayne is expired.");
		} catch (LoginException ex) {
			assertTrue(ex instanceof AccountExpiredException);
		}
	}

    @Test
	public void testLoginSuccess() throws Exception {
    	final AccountService authService = new AccountService();
    	authService.setPath("src/test/resources/jaas/passwd");

    	String config = "src/test/resources/jaas/jaas.conf";
		String alias = "embed";
		System.setProperty("java.security.auth.login.config", config);
		//java.security.Security.setProperty("auth.login.defaultCallbackHandler", "net.ec.auth.callback.ClientCallbackHandler");

		Subject subject;
		CallbackContext callbackContext = new CallbackContext()
				.setUsername("admin")
				.setPassword("password")
				.setAddress("127.0.0.1");
		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, authService);

		CallbackHandler callbackHandler = callbackContext.getCallbackHandler();
		assertNotNull(callbackHandler);

		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(alias, callbackHandler);
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

		    context.logout();

		    //After logout
		    subject = context.getSubject();
		    assertNotNull(subject);
		    assertTrue(subject.getPrincipals().isEmpty());
		    assertTrue(subject.getPrivateCredentials().isEmpty());
		    assertTrue(subject.getPublicCredentials().isEmpty());

		} catch (LoginException ex) {
			fail("admin login failed. " + ex.getMessage());
		}
	}

    @Test
	public void testLoginCallback() throws Exception {
    	final AccountService authService = new AccountService();
    	authService.setPath("src/test/resources/jaas/passwd");

		String config = "src/test/resources/jaas/jaas.conf";
		String alias = "embed";
		System.setProperty("java.security.auth.login.config", config);
		Subject subject = null;
		CallbackContext callbackContext = new CallbackContext()
				.setUsername("admin")
				.setPassword("password")
				.setAddress("127.0.0.1");
		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, authService);
		CallbackHandler callbackHandler = callbackContext.getCallbackHandler();

		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(alias, callbackHandler);
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
		} catch (LoginException ex) {
			fail("admin login failed. " + ex.getMessage());
		}

		try {
			assertNotNull(subject);
			LoginContext context = new LoginContext(alias, subject, callbackHandler);
		    context.logout();

		    //After logout
		    subject = context.getSubject();
		    assertNotNull(subject);
		    assertTrue(subject.getPrincipals().isEmpty());
		    assertTrue(subject.getPrivateCredentials().isEmpty());
		    assertTrue(subject.getPublicCredentials().isEmpty());

		} catch (LoginException ex) {
			fail("admin login failed. " + ex.getMessage());
		}
	}


}
