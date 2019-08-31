package net.tiny.ws;

import java.io.File;
import java.util.Date;

import com.sun.net.httpserver.Headers;

public final class ResponseHeaderHelper implements Constants {
    final Headers headers;
    protected ResponseHeaderHelper(Headers h) {
        headers = h;
    }

    public Headers getHeaders() {
        return headers;
    }

    public ResponseHeaderHelper setContentType(String file) {
    	MIME_TYPE mimeType = HttpHandlerHelper.getMimeType(file);
        if (null != mimeType) {
            setContentType(mimeType);
        }
        return this;
    }

    public ResponseHeaderHelper setContentType(MIME_TYPE mimeType) {
        headers.add(HEADER_CONTENT_TYPE, mimeType.getType());
        return this;
    }

    public ResponseHeaderHelper setContentLength(int length) {
        headers.add(HEADER_CONTENT_LENGTH, String.valueOf(length));
        return this;
    }

    public ResponseHeaderHelper setLastModified(File file) {
        headers.add(HEADER_LAST_MODIFIED, HttpDateFormat.format(new Date(file.lastModified())));
        return this;
    }

    public ResponseHeaderHelper enableCache(boolean enable) {
        if (!enable) {
            headers.add(HEADER_CACHE_CONTROL, "no-cache");
        }
        return this;
    }

    public ResponseHeaderHelper keepAlive(boolean keep) {
        if (keep) {
            headers.add(HEADER_CONNECTION, "keep-alive");
        }
        return this;
    }

    /**
     * Set a cookie for a HTTP session context
     *
     * @param cookie, full cookie  i.e. "foo=bar;fie;etc";
     */
    public ResponseHeaderHelper setCookie(String cookie) {
        headers.add(HEADER_SET_COOKIE, cookie);
        return this;
    }

    public ResponseHeaderHelper set(String name, String value) {
        headers.add(name, value);
        return this;
    }

}
