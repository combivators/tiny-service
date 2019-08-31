package net.tiny.ws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        parseQuery(query, parameters);
        exchange.setAttribute(HTTP_PARAMETER_ATTRIBUTE, parameters);
    }

    @SuppressWarnings("unchecked")
    private void parsePostParameters(HttpExchange exchange) throws IOException {
        Map<String, Object> parameters = (Map<String, Object>) exchange.getAttribute(HTTP_PARAMETER_ATTRIBUTE);
        BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
        String query = reader.readLine();
        parseQuery(query, parameters);
    }

    @SuppressWarnings("unchecked")
    private void parseQuery(String query, Map<String, Object> parameters) throws UnsupportedEncodingException {
        if (query != null) {
            String pairs[] = query.split("[&]");

            for (String pair : pairs) {
                String param[] = pair.split("[=]");

                String key = null;
                String value = null;
                if (param.length > 0) {
                    key = URLDecoder.decode(param[0], "UTF-8");
                }

                if (param.length > 1) {
                    value = URLDecoder.decode(param[1], "UTF-8");
                }

                if (parameters.containsKey(key)) {
                    Object obj = parameters.get(key);
                    if (obj instanceof List<?>) {
                        List<String> values = (List<String>) obj;
                        values.add(value);
                    } else if (obj instanceof String) {
                        List<String> values = new ArrayList<String>();
                        values.add((String) obj);
                        values.add(value);
                        parameters.put(key, values);
                    }
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }
}