package net.tiny.ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

/**
 *
 * <code>
 *  @Override
 *  public void handle(HttpExchange exchange) throws IOException {
 *      Map<String, Object> params =
 *         (Map<String, Object>)exchange.getAttribute("parameters");
 *        //now you can use the params
 *  }
 * </code>
 *
 */
public class ParameterFilter extends Filter implements Constants {

    @Override
    public String description() {
        return "Parses the requested URI for parameters";
    }

    @Override
    public void doFilter(HttpExchange he, Chain chain) throws IOException {
        switch (HTTP_METHOD.valueOf(he.getRequestMethod())) {
        case GET:
            parseGetParameters(he);
            break;
        case POST:
            parsePostParameters(he);
            break;
        default:
            break;
        }
        chain.doFilter(he);
    }

    private void parseGetParameters(HttpExchange exchange) throws UnsupportedEncodingException {
        Map<String, Object> parameters = new HashMap<String, Object>();
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        PostParameterPaser.parseQuery(query, parameters);
        exchange.setAttribute(HTTP_PARAMETER_ATTRIBUTE, parameters);
    }

    @SuppressWarnings("unchecked")
    private void parsePostParameters(HttpExchange exchange) throws IOException {
        Map<String, Object> parameters = (Map<String, Object>) exchange.getAttribute(HTTP_PARAMETER_ATTRIBUTE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
        String query = reader.readLine();
        PostParameterPaser.parseQuery(query, parameters);
    }
}