package net.tiny.ws.auth;

import java.time.LocalDateTime;
import java.util.Set;

import javax.security.auth.login.LoginException;

public interface AuthenticationService {
	Set<String> getRoles(String username);
	void verify(String user, String password, String address, LocalDateTime time) throws LoginException;
}
