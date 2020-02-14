package net.tiny.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.util.Optional;
import java.util.logging.Level;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;

public abstract class BaseWebService extends AbstractWebService implements Constants {

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR" };
    private static final String DEFAULT_ALLOWED_METHODS  = "GET, POST, PUT, DELETE, OPTIONS";
    private static final String GET_ONLY_ALLOWED_METHODS = "GET, OPTIONS";

    abstract protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException;


    @Override
    public void handle(HttpExchange he) throws IOException {
        try {
            final Headers headers = he.getResponseHeaders();
            final String allowedMethods = getAllowedMethods();
            final HTTP_METHOD method = HTTP_METHOD.valueOf(he.getRequestMethod());

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("%s : %s '%s' %s",
                    getClass().getSimpleName(), method.name(),
                    he.getRequestURI().getPath(), (isAllowedMethod(method) ? "Allow" : "Deny")));
            }

            switch (method) {
            case GET:
            case POST:
            case PUT:
            case DELETE:
                if (isAllowedMethod(method)) {
                    execute(method, he);
                } else {
                    headers.set(HEADER_ALLOW, allowedMethods);
                    he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, NO_RESPONSE_LENGTH);
                }
                break;
            case OPTIONS:
                headers.set(HEADER_ALLOW, allowedMethods);
                he.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_RESPONSE_LENGTH);
                break;
            default:
                headers.set(HEADER_ALLOW, allowedMethods);
                he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, NO_RESPONSE_LENGTH);
                break;
            }
        } catch (RuntimeException | IOException ex) {
            he.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, NO_RESPONSE_LENGTH);
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        } finally {
            he.close();
        }
    }

    public void publish(HttpContext serverContext) {
        // Not endpoint do nothing.
    }

    protected boolean doGetOnly() {
        return false;
    }

    protected String getAllowedMethods() {
        return doGetOnly() ? GET_ONLY_ALLOWED_METHODS : DEFAULT_ALLOWED_METHODS;
    }

    protected boolean isAllowedMethod(HTTP_METHOD method) {
        return doGetOnly() ? "GET".equals(method.name()) : getAllowedMethods().contains(method.name());
    }

    protected final byte[] readResource(String resource) throws IOException {
        InputStream is = getClass().getResourceAsStream(resource);
        if (is == null) {
            throw new IllegalArgumentException(String.format("Not found '%s'", resource));
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int nread;
        while ((nread = is.read(buffer)) > 0) {
            baos.write(buffer, 0, nread);
        }
        baos.close();
        is.close();
        return baos.toByteArray();
    }

    protected final String loadResource(String resource) throws IOException {
        return new String(readResource(resource));
    }

    public static <V> V getClientAddress(HttpExchange he, Class<V> paramType) {
        Optional<String> remote = getClientAddress(he.getRequestHeaders());
        if (remote.isPresent() && paramType.equals(String.class)) {
            return paramType.cast(remote.get());
        }
        InetAddress address = he.getRemoteAddress().getAddress();
        if (paramType.equals(InetAddress.class)) {
            return paramType.cast(address);
        } else {
            return paramType.cast(address.getHostAddress());
        }
    }

    static Optional<String> getClientAddress(Headers headers) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = headers.getFirst(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                return Optional.of(ip);
            }
        }
        return Optional.empty();
    }
}
