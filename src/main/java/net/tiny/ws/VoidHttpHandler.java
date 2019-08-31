package net.tiny.ws;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.sun.net.httpserver.HttpExchange;

public final class VoidHttpHandler extends BaseWebService {

	@Override
	protected boolean doGetOnly() {
		return true;
	}

	@Override
	protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
        he.close();
    }

}
