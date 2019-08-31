package net.tiny.ws.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.config.JsonParser;
import net.tiny.ws.BaseWebService;
import net.tiny.ws.HttpHandlerHelper;
import net.tiny.ws.RequestHelper;
import net.tiny.ws.ResponseHeaderHelper;

public class PostPutOptionHandler extends BaseWebService {

    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @Override
    protected String getAllowedMethods() {
        return "POST, PUT, DELETE, OPTIONS";
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        switch (method) {
        case POST:
            String entity = new String(request.getRequestContent());
            DummyEmail mail = JsonParser.unmarshal(entity, DummyEmail.class);
            int pos = mail.email.indexOf("@");
            DummyUser user = new DummyUser();
            user.name = mail.email.substring(0, pos);
            user.domain = mail.email.substring(pos+1);
            header.setContentType(MIME_TYPE.JSON);
            String response = JsonParser.marshal(user);
            final byte[] rawResponseBody = response.getBytes(CHARSET);
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponseBody.length);
            he.getResponseBody().write(rawResponseBody);
            break;
        case PUT:
            String req = new String(request.getRequestContent());
            DummyEmail email = JsonParser.unmarshal(req, DummyEmail.class);
            header.setContentType(MIME_TYPE.JSON);
            String json = JsonParser.marshal(email);
            final byte[] res = json.getBytes(CHARSET);
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, res.length);
            he.getResponseBody().write(res);
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_RESPONSE_LENGTH);
            break;
        case DELETE:
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_RESPONSE_LENGTH);
            break;
        case OPTIONS:
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, NO_RESPONSE_LENGTH);
            break;
        default:
            header.set(HEADER_ALLOW, getAllowedMethods());
            he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, NO_RESPONSE_LENGTH);
            break;
        }
    }

    public static class DummyEmail {
        public String email;
    }

    public static class DummyUser {
        public String name;
        public String domain;
    }
}
