package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HttpHandlerHelperest {

    @Test
    public void testGetMimeType() throws Exception {
        assertEquals("text/html; charset=utf-8", HttpHandlerHelper.getMimeType("index.html").getType());
        assertEquals("text/css; charset=utf-8", HttpHandlerHelper.getMimeType("style.css").getType());
        assertEquals("text/javascript; charset=utf-8", HttpHandlerHelper.getMimeType("jquery.js").getType());
        assertEquals("image/vnd.microsoft.icon", HttpHandlerHelper.getMimeType("favicon.ico").getType());
        assertEquals("image/jpeg", HttpHandlerHelper.getMimeType("picture.jpg").getType());
        assertEquals("image/svg+xml", HttpHandlerHelper.getMimeType("cloud.svg").getType());
        assertEquals("text/xml; charset=utf-8", HttpHandlerHelper.getMimeType("define.xml").getType());
        assertEquals("application/json; charset=utf-8", HttpHandlerHelper.getMimeType("product.json").getType());
    }
}
