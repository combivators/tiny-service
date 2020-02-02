package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Collection;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

public class ControllableHandler extends BaseWebService {

    private static final String REQEST_REGEX = "(^status|^start|^stop|^suspend|^resume)([&][0-9a-zA-Z_-]+)*";

    enum Command {
        status,
        start,
        stop,
        suspend,
        resume
    }


    @Override
    protected boolean doGetOnly() {
        return true;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        String req = request.getParameter(0);
        if (!isVaildRequest(req)) {
            // Not found
            he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
        } else {
            String[] args = req.split("[&]");

            try {
                String response = query(args);
                if( null != response) {
                    byte[] buffer = response.getBytes();
                    he.sendResponseHeaders(HttpURLConnection.HTTP_OK, buffer.length);
                    he.getResponseBody().write(buffer);
                } else {
                    he.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                }
            } catch (UnsupportedOperationException e) {
                he.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, -1);
            }
        }
    }

    static boolean isVaildRequest(String req) {
        return Pattern.matches(REQEST_REGEX, req);
    }

    String query(String[] args) {
        Collection<AutoCloseable> launchers = context.lookupGroup(AutoCloseable.class);
        if (null == launchers)
            return null;

        StringBuilder response = new StringBuilder();
        Command command = Command.valueOf(args[0].toLowerCase());
        switch (command) {
        case status:
            for (AutoCloseable c : launchers) {
                if (response.length() > 0) {
                    response.append(", ");
                }
                response.append(c.toString());
            }
            response.append(" running...");
            break;
        case stop:
            Throwable err = null;

            for (AutoCloseable c : launchers) {
                if (response.length() > 0) {
                    response.append(", ");
                }
                try {
                    c.close();
                    response.append(c.toString());
                } catch (Exception e) {
                    err = e;
                }
            }
            response.append(" stopping...");
            if( err != null) {
                response.append(" Error:").append(err.getMessage());
            }
            break;
        case start:
        case suspend:
        case resume:
            response.append(String.format("Unsupport command '%s'.", command.name()));
            break;
        }
        return response.toString();
    }

    <T> T lookup(String name, Class<T> type) {
        if (context == null)
            return null;
        return context.lookup(name, type);
    }
}
