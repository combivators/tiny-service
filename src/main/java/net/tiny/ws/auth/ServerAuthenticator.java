package net.tiny.ws.auth;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.sun.net.httpserver.BasicAuthenticator;

public class ServerAuthenticator extends BasicAuthenticator {

	final static String REALM = "default";
	private AuthenticationService service;

	public ServerAuthenticator() {
		super(REALM);
	}

	@Override
	public boolean checkCredentials(String user, String password) {
		CallbackContext callbackContext = new CallbackContext()
				.setUsername(user)
				.setPassword(password)
				.setAddress("0.0.0.0");

		callbackContext.setCallbackHandler(new DefaultCallbackHandler());
		callbackContext.setTarget(AuthenticationService.class, service);
		CallbackHandler callbackHandler = callbackContext.getCallbackHandler();
		try {
			// Create a LoginContext with a callback handler
			LoginContext context = new LoginContext(getRealm(), callbackHandler);
			// Perform authentication
		    context.login();
		    return true;
		} catch (LoginException ex) {
			return false;
		}
	}


	public void setService(AuthenticationService service) {
		this.service = service;
	}
}
