package net.tiny.ws;

import java.io.IOException;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

public class CorsResponseFilter extends Filter implements Constants {

    @Override
    public String description() {
        return "HTTP CORS Filter";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        if (null != chain) {
            chain.doFilter(exchange);
        }
        switch (HTTP_METHOD.valueOf(exchange.getRequestMethod())) {
        case GET:
        case POST:
        case PUT:
        case DELETE:
        	setResponseHeader(HttpHandlerHelper.getHeaderHelper(exchange));
            break;
        default:
            break;
        }
    }

    private void setResponseHeader(ResponseHeaderHelper header) {
        header.set("Access-Control-Allow-Origin", "*");
        header.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        header.set("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With");
        header.set("Access-Control-Allow-Credentials", "true");
    }
}
