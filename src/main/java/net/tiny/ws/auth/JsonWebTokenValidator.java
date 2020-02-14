package net.tiny.ws.auth;

import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import net.tiny.service.Patterns;

public class JsonWebTokenValidator implements Supplier<BiPredicate<String,String>> {

    private static Logger LOGGER = Logger.getLogger(JsonWebTokenFilter.class.getName());

    protected Supplier<String> publicKey;
    protected Supplier<Patterns> patterns;

    @Override
    public BiPredicate<String, String> get() {
        return (String uri, String auth) -> verify(uri, auth);
    }

    boolean verify(String uri, String auth) {
        boolean targeted = getPatterns().vaild(uri);
        if (targeted) {
            // When HTTP path is requested to check JWT
            return verify(auth);
        }
        return true;
    }

    private boolean verify(final String auth) {
        if (null == auth) {
           return false;
        }
        final String[] bearer  = auth.split(" ");
        if (bearer.length != 2 || !"Bearer".equalsIgnoreCase(bearer[0])) {
           return false;
        }
        final JsonWebToken token = JsonWebToken.valueOf(bearer[1]);
        LOGGER.info(String.format("[JWT] expired:%s  claims: '%s' ", token.expired(), token.claims()));
        if (!token.expired()) {
            return token.verify(getPublicKey());
        } else {
            return false;
        }
    }

    protected Patterns getPatterns() {
        return patterns.get();
    }

    protected String getPublicKey() {
        return publicKey.get();
    }
}
