package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

public class TestJsonHandler extends BaseWebService {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @Override
    protected boolean doGetOnly() {
        return true;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        // Do GET method only
        RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final Map<String, List<String>> requestParameters = request.getParameters();
        // do something with the request parameters
        final String responseBody = "['hello world!']";

        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        header.setContentType(MIME_TYPE.JSON);
        final byte[] rawResponseBody = responseBody.getBytes(CHARSET);
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponseBody.length);
        he.getResponseBody().write(rawResponseBody);
    }

}
