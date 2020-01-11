package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.auth.SecureKey;

/**
 * Call 'http://localhost/secure/public' to send server public key.
 * Secure key will be hold in session.
 */
public class SecureHttpHandler extends BaseWebService {

    private ServerRepository repository;

    @Override
    protected boolean doGetOnly() {
        return true;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange exchange) throws IOException {
        if (null == repository) {
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
            exchange.close();
            return;
        }
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(exchange);
        createSecureKey(request, exchange);
    }

    public void setServerRepository(ServerRepository repository) {
        this.repository = repository;
    }


    void createSecureKey(RequestHelper request, HttpExchange he) throws IOException {
        // If has valid session, get the secure key from session.
        String sessionId = request.getCookie(COOKIE_SESSION, true);
        ServerSession session = null;
        if (null != sessionId) {
            Optional<ServerSession> opt = repository.getSession(sessionId);
            if(opt.isPresent()) {
                session = opt.get();
            }
        }
        if (null == session) {
            // Create a new session
            session = repository.createSession();
        }
        SecureKey secureKey = session.getAttribute(SecureKey.class);
        if (null == secureKey) {
            // Create a new secure key and save to this session.
            secureKey = new SecureKey();
            session.setAttribute(SecureKey.class, secureKey);
        }

        // Send json data of public key to client. no-cache, no-store.
        final Map<String,String> publicKeys = secureKey.createPublicKeys();
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        header.setCookie(String.format("%s=%s;path=/", COOKIE_SESSION, session.getId()));
        header.setContentType(MIME_TYPE.JSON);
        header.set("Cache-Control", "no-cache, no-store, must-revalidate");
        header.set("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
        final String response = JsonParser.marshal(publicKeys);
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length());
        he.getResponseBody().write(response.getBytes());
        he.close();
    }
}
