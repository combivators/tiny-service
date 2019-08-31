package net.tiny.ws;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ServerRepository {

	// Session expiration timeout.
	private static final long DEFALUT_SESSION_TIMEOUT = Duration.ofMinutes(5).toMillis();

    /*
     * SecureRandom guarantee secure random strings, very hard to hack, but
     * the initialization is expensive and for this reason we are making
     * it static.
     */
    private static SecureRandom random = new SecureRandom();

	private long sessionTimeout = DEFALUT_SESSION_TIMEOUT;
	private final ConcurrentHashMap<String, ServerSession> sessionStore = new ConcurrentHashMap<>(8, 0.9f, 1);

	public long getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(long timeout) {
		sessionTimeout = timeout;
	}

    public ServerSession createSession() {
        ServerSession session = new ServerSession(new BigInteger(130, random).toString(32), sessionTimeout);
        sessionStore.put(session.getId(), session);
        return session;
    }

    public Optional<ServerSession> getSession(String session) {
        ServerSession success = sessionStore.get(session);
        return (null != success && success.isValid()) ? Optional.of(success.keepAlive()) : Optional.empty();
    }

    public Optional<ServerSession> closeSession(String session) {
        ServerSession success = sessionStore.remove(session);
        return null != success ? Optional.of(success) : Optional.empty();
    }

    /*
     * Clean the session store from expired session key
     */
    public void cleanupSession() {
    	Collection<ServerSession> sessions = sessionStore.values();
    	sessions.stream()
    		.filter(s -> !s.isValid())
    		.forEach(s -> sessionStore.remove(s.getId()));
    }
}
