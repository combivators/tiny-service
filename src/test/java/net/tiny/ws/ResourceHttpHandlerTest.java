package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class ResourceHttpHandlerTest {

    @Test
    public void testFindHome() throws Exception {
        URL url = new URL("http://localhost:8080/css/style.css");
        String uri = url.toURI().getPath();
        assertEquals("/css/style.css", uri);
        String context = uri.substring(1, uri.indexOf("/", 1));
        assertEquals("css", context);
        assertEquals("/style.css", uri.substring(uri.indexOf("/", 1)));

        url = new URL("http://localhost:8080/index.html");
        uri = url.toURI().getPath();
        assertEquals("/index.html", uri);
        assertEquals(-1, uri.indexOf("/", 1));
    }

    @Test
    public void testHomeAndMapping() throws Exception {
        ResourceHttpHandler rh = new ResourceHttpHandler();
        Map<String, String> resources = new HashMap<>();
        resources.put("/", "src/test/resources/home");
        resources.put("css", "src/test/resources/home/css");
        rh.setResources(resources);
        assertNotNull(rh.findLocalFile("/index.html"));
        assertNotNull(rh.findLocalFile("/css/style.css"));

        assertNull(rh.findLocalFile("/css/xyz.css"));
        assertNull(rh.findLocalFile("/img/abc.png"));
        assertNull(rh.findLocalFile("/unkonw.htm"));
    }

    @Test
    public void testHomeMappingOnly() throws Exception {
        ResourceHttpHandler rh = new ResourceHttpHandler();
        Map<String, String> resources = new HashMap<>();
        resources.put("/", "src/test/resources/home");
        rh.setResources(resources);

        assertNotNull(rh.findLocalFile("/index.html"));
        assertNotNull(rh.findLocalFile("/css/style.css"));

        assertNull(rh.findLocalFile("/css/xyz.css"));
        assertNull(rh.findLocalFile("/img/abc.png"));
        assertNull(rh.findLocalFile("/unkonw.htm"));
    }

    @Test
    public void testLocalMappingOnly() throws Exception {
        ResourceHttpHandler rh = new ResourceHttpHandler();
        Map<String, String> resources = new HashMap<>();
        resources.put("css", "src/test/resources/home/css");
        rh.setResources(resources);

        assertNotNull(rh.findLocalFile("/css/style.css"));
        assertNull(rh.findLocalFile("/index.html"));
        assertNull(rh.findLocalFile("/css/xyz.css"));
        assertNull(rh.findLocalFile("/img/abc.png"));
        assertNull(rh.findLocalFile("/unkonw.htm"));
    }

    @Test
    public void testHomeAndResouceMapping() throws Exception {
        ResourceHttpHandler rh = new ResourceHttpHandler();
        Map<String, String> resources = new HashMap<>();
        resources.put("/", "home");
        resources.put("css", "home/css");
        rh.setResources(resources);

        assertNotNull(rh.findResouce("/index.html"));
        assertNotNull(rh.findResouce("/"));
        assertTrue(rh.findResouce("/").toString().endsWith("index.html"));
        assertNotNull(rh.findResouce("/css/style.css"));

        assertNull(rh.findResouce("/css/xyz.css"));
        assertNull(rh.findResouce("/img/abc.png"));
        assertNull(rh.findResouce("/unkonw.htm"));
    }
}
