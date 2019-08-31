package net.tiny.ws;

public interface Constants {

    String DEFALUT_SERVER_NAME   = "Embedded Server v1.0";
    String COOKIE_SESSION   = "JSESSIONID";
    String COOKIE_AUTHTOKEN = "AT";
    String HTTP_PARAMETER_ATTRIBUTE = "parameters";
    byte[] NOT_FOUND    = "<!DOCTYPE html><html><head><title>404 - Not Found</title></head><body>404 - Not Found</body></html>".getBytes();
    byte[] SERVER_ERROR = "<!DOCTYPE html><html><head><title>500 - Error</title></head><body>500 - Error</body></html>".getBytes();
    String HEADER_ALLOW          = "Allow";
    String HEADER_LAST_MODIFIED  = "Last-Modified";
    String HEADER_CONTENT_TYPE   = "Content-Type";
    String HEADER_CONTENT_LENGTH = "Content-Length";
    String HEADER_SET_COOKIE     = "Set-Cookie";
    String HEADER_CACHE_CONTROL  = "Cache-Control";
    String HEADER_CONNECTION     = "Connection";

    int NO_RESPONSE_LENGTH = -1;
    int DEFAULT_BUFFER_SIZE = 8192;

    enum HTTP_METHOD {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE,
        OPTIONS,
        HEAD,
        TRACE,
        LINK,
        UNLINK
    }

    String MIME_TYPE_HTML  = "text/html; charset=utf-8";
    String MIME_TYPE_JSON  = "application/json; charset=utf-8";

    enum MIME_TYPE {
        JSON(MIME_TYPE_JSON),
        HTML(MIME_TYPE_HTML),
        HTM(MIME_TYPE_HTML),
        XML("text/xml; charset=utf-8"),
        CSS("text/css; charset=utf-8"),
        JS("text/javascript; charset=utf-8"),
        TXT("text/plain"),
        CSV("text/csv"),
        PDF("application/pdf"),
        // Image
        JPG("image/jpeg"),
        GIF("image/gif"),
        PNG("image/png"),
        TIFF("image/tiff"),
        SVG("image/svg+xml"),
        ICO("image/vnd.microsoft.icon"),
        DAT("application/octet-stream");

        private final String type;
        MIME_TYPE(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
