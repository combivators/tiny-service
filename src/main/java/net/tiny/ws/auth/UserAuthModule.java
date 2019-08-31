package net.tiny.ws.auth;

import java.time.LocalDateTime;
import java.util.Set;

import javax.security.auth.login.LoginException;

public class UserAuthModule extends BaseModule {


 	public static final String AUTH_MODULE = DEFAULT_AUTH_MODULE;

 	private static long ALIVE_TIMEOUT = -1L;

	@Override
	protected UserToken verify(String username, char[] password, String address, LocalDateTime time) throws LoginException {
		try {
			final AuthenticationService service = getCallbackContext().getTarget(AuthenticationService.class);
			if(null == service) {
				throw new LoginException("Can not found a implemented authentication service.");
			}
			String pass = new String(password);
			service.verify(username, pass, address, time); // If fail throw LoginException
			int credential = pass.hashCode();
			UserToken userToken = new UserToken(username, credential);
			userToken.setAddress(address);

			//
			boolean remember = getCallbackContext().getRemember();
			long expired = System.currentTimeMillis();
			if(remember) {
				//unlimit
				expired += REMEMBER_ALIVE;
			} else {
				expired += getExpired();
			}
			userToken.keepAlive(expired); // Call getToken to use Crypt.encrypt

			Set<String> roleSet  = service.getRoles(username);
			userToken.setRoles(roleSet);
			return userToken;
		} catch (LoginException ex) {
			if(isDebug()) {
				LOGGER.warning(String.format("[JAAS] %s", ex.getMessage()));
			}
			throw ex;
		}
	}

	private long getExpired() {
		if( ALIVE_TIMEOUT == -1L) {
			String value = super.getOption(KEY_EXPIRED, Long.toString(UserToken.TIME_OUT));
			ALIVE_TIMEOUT = Long.parseLong(value);
		}
		return ALIVE_TIMEOUT;
	}
}
