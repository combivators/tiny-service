package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

public class ControllableHandler extends BaseWebService implements ControllerService {

    private Controllable server;

    enum Command {
        status,
        start,
        stop,
        suspend,
        resume
    }

    public void setControllable(Controllable controller) {
    	this.server = controller;
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
        // Default is embedded server controller
        Controllable controller = server;
        if (args.length > 1) {
            controller = lookup(args[1], Controllable.class);
        }

        String response = null;
        if (null == controller)
        	return response;

        Command command = Command.valueOf(args[0].toLowerCase());
        switch (command) {
        case status:
            response = controller.status();
            break;
        case start:
            controller.start();
            response = "starting";
            break;
        case stop:
            controller.stop();
            response = "stopping";
            break;
        case suspend:
            controller.suspend();
            break;
        case resume:
            controller.resume();
            break;
        }
        return response;
    }
}
