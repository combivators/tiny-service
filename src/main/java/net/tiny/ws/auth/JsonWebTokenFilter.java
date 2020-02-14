package net.tiny.ws.auth;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import net.tiny.service.Patterns;


public class JsonWebTokenFilter extends Filter {

    private JsonWebTokenValidator validator = new JsonWebTokenValidator();
    private String uri = "/api/.*";
    private Path key;

    @Override
    public String description() {
        return "Json Web Token auth filter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        final String requestURI = exchange.getRequestURI().toString();
        // See HTPP header "Authorization: Bearer {jwt}"
        final String auth = exchange.getRequestHeaders().getFirst("Authorization");
        if (!validator.get().test(requestURI, auth)) {
            // 401 : Unauthorized
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, -1);
            return;
        }
        if (null != chain) {
            chain.doFilter(exchange);
        }
    }

    public void setKey(Path k) {
        key = k;
        if (!Files.exists(key)) {
            throw new IllegalArgumentException("Not found public key file : " + key);
        }
        try {
            final String publicKey = new String(Files.readAllBytes(key));
            validator.publicKey = () -> publicKey;
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not read public key file : " + key);
        }
    }


    public void setUri(String u) {
        uri = u;
        validator.patterns = () -> Patterns.valueOf(uri);
    }

}
