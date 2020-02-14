package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import net.tiny.service.Patterns;

public class BlacklistFilter extends Filter {

    private static Logger LOGGER = Logger.getLogger(BlacklistFilter.class.getName());

    private Supplier<String> denied = null;
    private Patterns blacklist;

    @Override
    public String description() {
        return "Access backlist filter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        final String clientAddress = BaseWebService.getClientAddress(exchange, String.class);
        if (isBlacklist(clientAddress)) {
            // 403 : Forbidden
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_FORBIDDEN, -1);
            LOGGER.warning(String.format("[AUTH] Black client '%s' forbidding.", clientAddress));
            return;
        }
        if (null != chain) {
            chain.doFilter(exchange);
        }
    }

    private boolean isBlacklist(String client) {
        if (denied == null)
            return false;
        return getDeniedPatterns().vaild(client);
    }

    private Patterns getDeniedPatterns() {
        if (blacklist == null) {
            blacklist = Patterns.valueOf(denied.get());
        }
        return blacklist;
    }


    public void setDenied(Supplier<String> s) {
        denied = s;
    }
}
