package net.tiny.ws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import com.sun.net.httpserver.HttpExchange;

public final class HttpHandlerHelper implements Constants {

    ///////////////////////////////////////////
    // Static public methods
    public static MIME_TYPE getMimeType(String file) {
        if (file == null || file.isEmpty())
            return null;
        final String suffix = file.substring(file.lastIndexOf(".") + 1);
        if(suffix == null)
            return null;
        return MIME_TYPE.valueOf(MIME_TYPE.class, suffix.toUpperCase());
    }

    public static ResponseHeaderHelper getHeaderHelper(HttpExchange he) {
        return new ResponseHeaderHelper(he.getResponseHeaders());
    }

    public static RequestHelper getRequestHelper(HttpExchange he) {
        return new RequestHelper(he);
    }

    public static byte[] getRequestBody(HttpExchange he) throws IOException {
        InputStream is = he.getRequestBody();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        sendChunk(is, os);
        is.close();
        os.close();
        return os.toByteArray();
    }

    public static String getRequestMessage(HttpExchange he) throws IOException {
        return URLDecoder.decode(new String(getRequestBody(he)), "UTF-8");
    }

    private static void sendChunk(InputStream is, OutputStream os)
            throws IOException {
        int size = is.available();
        int bufferSize = Math.min(size, DEFAULT_BUFFER_SIZE);
        byte[] buffer = new byte[bufferSize];
        int readBytes = -1;
        while ((readBytes = is.read(buffer)) > 0) {
            os.write(buffer, 0, readBytes);
        }
    }
}
