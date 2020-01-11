package net.tiny.ws;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;


public class SnapHttpHandler extends BaseWebService {

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        switch (method) {
        case GET:
            doGet(he);
            break;
        case POST:
            doPost(he);
            break;
        case PUT:
            doPut(he);
            break;
        case DELETE:
            doDelete(he);
            break;
        default:
            break;
        }
    }

    void doGet(HttpExchange he) throws IOException {
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        StringBuffer buffer = new StringBuffer();
        Set<String> keySet = header.getHeaders().keySet();
        Iterator<String> iter = keySet.iterator();
        while (iter.hasNext()){
            String key = iter.next();
            List<String> values = header.getHeaders().get(key);
            buffer.append(key + " = " + values.toString() + "\n");
        }
        String res = buffer.toString();
        header.setContentLength(res.length());
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, res.length());
        OutputStream out = he.getResponseBody();
        out.write(res.getBytes());
        out.close();
    }

    void doPost(HttpExchange he) throws IOException {
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        String request = new String(HttpHandlerHelper.getRequestBody(he));
        header.setContentLength(request.length());
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, request.length());
        OutputStream out = he.getResponseBody();
        out.write(request.getBytes());
        out.close();
    }

    void doPut(HttpExchange he) throws IOException {

    }

    void doDelete(HttpExchange he) throws IOException {

    }



}
