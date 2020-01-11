package net.tiny.ws;

import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

public class SnapFilter extends Filter {
    private static final Logger LOGGER = Logger.getLogger(SnapFilter.class.getName());
    private static final String CRLF = "\r\n";

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        String request = getHttpRequestHeader(exchange);
        LOGGER.info(request);
        if (null != chain) {
            chain.doFilter(exchange);
        }
        String response = getHttpResponseHeader(exchange);
        LOGGER.info(response);
    }

    @Override
    public String description() {
        return "HTTP Snoop Filter";
    }

    private String getHttpRequestHeader(HttpExchange exchange) {
        StringBuffer sb = new StringBuffer();
        sb.append(">> [HTTP-REQ] >>");
        sb.append(CRLF);
        sb.append(exchange.getRequestMethod());
        sb.append(" ");
        sb.append(exchange.getRequestURI());
        sb.append(" ");
        sb.append(exchange.getProtocol());
        sb.append(CRLF);
        Headers requestHeaders = exchange.getRequestHeaders();
        sb.append(headersToString(requestHeaders));
        return sb.toString();
    }

    private String getHttpResponseHeader(HttpExchange exchange) {
        StringBuffer sb = new StringBuffer();
        sb.append("<< [HTTP-RES] <<");
        sb.append(CRLF);
        sb.append("HTTP/1.1 ");
        sb.append(exchange.getResponseCode());
        sb.append(CRLF);
        Headers responseHeaders = exchange.getResponseHeaders();
        sb.append(headersToString(responseHeaders));
        return sb.toString();
    }

    private String headersToString(Headers headers) {
        StringBuffer sb = new StringBuffer();
        Iterator<String> it = headers.keySet().iterator();
        while (it.hasNext()) {
            String name = it.next();
            String value = headers.getFirst(name);
            sb.append(name);
            sb.append(": ");
            sb.append(value);
            sb.append(CRLF);
        }
        sb.append(CRLF);
        return sb.toString();
    }
}
