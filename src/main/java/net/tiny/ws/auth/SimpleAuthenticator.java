package net.tiny.ws.auth;

import java.util.function.Predicate;
import java.util.logging.Logger;

import com.sun.net.httpserver.BasicAuthenticator;

public class SimpleAuthenticator extends BasicAuthenticator {

    private static final Logger LOGGER = Logger.getLogger(SimpleAuthenticator.class.getName());
    static final String DEFAULT_ALGORITHM = "DES";
    static final String REALM = "simple";

    private String algorithm = DEFAULT_ALGORITHM;
    private String token = null;
    private String username;
    private String password;
    private boolean encode = true;
    private Predicate<String> vaildUser = null;
    private Predicate<String> vaildPassword = null;

    public SimpleAuthenticator() {
        super(REALM);
    }

    @Override
    public boolean checkCredentials(String user, String password) {
        if ( null == vaildUser || null == vaildPassword)
            return false;
        return vaildUser.test(user) && vaildPassword.test(password);
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public void setToken(String secure) {
        this.token = secure;
        Crypt.apply(token);
    }

    public void setUsername(String user) {
        this.username = user;
        this.vaildUser = Predicate.isEqual(this.username);
    }

    public void setPassword(String pass) {
        this.password = pass;
        if (this.encode) {
            try {
                this.vaildPassword = Predicate.isEqual(Crypt.create(algorithm).decrypt(this.password));
            } catch (SecurityException e) {
                this.vaildPassword = null;
                LOGGER.warning(String.format("Must input encoded password. - '%s'", e.getMessage()));
            }
        } else {
            this.vaildPassword = Predicate.isEqual(this.password);
        }
    }

    public void setEncode(boolean enable) {
        this.encode = enable;
        if(null != this.password) {
            if (this.encode) {
                try {
                    this.vaildPassword = Predicate.isEqual(Crypt.create(algorithm).decrypt(this.password));
                } catch (SecurityException e) {
                    this.vaildPassword = null;
                    LOGGER.warning(String.format("Must input encoded password. - '%s'", e.getMessage()));
                }
            } else {
                this.vaildPassword = Predicate.isEqual(this.password);
            }
        }
    }
}
