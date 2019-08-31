package net.tiny.ws.auth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.security.auth.login.AccountExpiredException;
import javax.security.auth.login.AccountLockedException;
import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

public class AccountService implements AuthenticationService {

    private String path;
    private ConcurrentMap<String, UserToken> users = new ConcurrentHashMap<>();
    private Set<String> locked = Collections.synchronizedSet(new HashSet<>());
    private long lastModified = 0L;

	@Override
	public Set<String> getRoles(String username) {
		final UserToken userToken = users.get(username);
		if (null != userToken)
			return userToken.getRoles();
		else
			return Collections.emptySet();
	}

	@Override
	public void verify(String username, String password, String address, LocalDateTime time) throws LoginException {
		final UserToken userToken = users.get(username);
		if(null == userToken) {
			throw new AccountNotFoundException(String.format("Not found account '%s'", username));
		}
		if(userToken.isExpired()) {
			throw new AccountExpiredException(String.format("Account '%s' expired", username));
		}
		if(locked.contains(username)) {
			throw new AccountLockedException(String.format("Account '%s' locked", username));
		}
		int credential = new String(password).hashCode();
		if(credential != userToken.getCredential()) {
			throw new FailedLoginException(String.format("Account '%s' login failed", username));
		}
		userToken.setAddress(address);
		userToken.keepAlive(); // Call getToken to use Crypt.encrypt
	}


	public void setPath(String path) {
		this.path = path;
		final File file = new File(this.path);
        if (!file.exists()) {
            throw new RuntimeException(String.format("Must configure password file '%1$s' in JAAS", file.getAbsolutePath()));
        }
        try {
        	load(file);
        } catch (IOException ex) {
        	throw new RuntimeException(String.format("Load password file '%1$s' error. Cause:%2$s", file.getAbsolutePath(), ex.getMessage()));
        }
	}

    private void load(File file) throws IOException {
    	if(!users.isEmpty()
    		&& lastModified > 0L
    		&& lastModified == file.lastModified()) {
    		return;
    	}
    	users.clear();
    	locked.clear();
    	lastModified = file.lastModified();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            if(line.startsWith("#")) {
            	continue;
            }
            String[] values = line.split(",");
            UserToken u = new UserToken(values[0], values[1].hashCode());
            // Set roles
            if(values.length > 4) {
	            Set<String> roleSet = new HashSet<>();
	            for(int i=4; i<values.length; i++) {
	            	roleSet.add(values[i]);
	            }
	            u.setRoles(roleSet);
            }
            if(!values[2].equals("0")) { //Is locked?
            	locked.add(u.getUsername());
            }
            if(!values[3].equals("0")) { // Set expired
            	if(Long.parseLong(values[3]) < 0L) {
            		u.expired();
            	}
            }
            users.put(u.getUsername(), u);
        }
        reader.close();
    }
}
