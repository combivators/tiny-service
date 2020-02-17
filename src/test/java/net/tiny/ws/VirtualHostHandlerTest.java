package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class VirtualHostHandlerTest {


    @Test
    public void testMatchetRelativePath() throws Exception {
        String regex = "[/]*[.][.]/.*";
        assertFalse(Pattern.matches(regex, "/index.html"));
        assertFalse(Pattern.matches(regex, "/./index.html"));
        assertFalse(Pattern.matches(regex, "/css/style.css"));
        assertFalse(Pattern.matches(regex, "/css/./style.css"));

        assertTrue(Pattern.matches(regex, "/../index.html"));
        assertTrue(Pattern.matches(regex, "../index.html"));
    }
    @Test
    public void testGetVirtualHost() throws Exception {
        VirtualHostHandler handler = new VirtualHostHandler();
        List<VirtualHost> hosts = new ArrayList<>();
        hosts.add(new VirtualHost()
                .domain("one.localdomain")
                .home("src/test/resources/virtual/one"));
        hosts.add(new VirtualHost()
                .domain("two.localdomain")
                .home("src/test/resources/virtual/two"));
        hosts.add(new VirtualHost()
                .domain("three.localdomain:8081")
                .home("src/test/resources/virtual/three-8081"));
        hosts.add(new VirtualHost()
                .domain("three.localdomain:8082")
                .home("src/test/resources/virtual/three-8082"));
        handler.setHosts(hosts);

        assertEquals("src/test/resources/virtual/one", handler.findVirtualHost("one.localdomain"));
        assertEquals("src/test/resources/virtual/one", handler.findVirtualHost("one.localdomain:8081"));
        assertEquals("src/test/resources/virtual/two", handler.findVirtualHost("two.localdomain"));
        assertEquals("src/test/resources/virtual/two", handler.findVirtualHost("two.localdomain:8082"));
        assertEquals("src/test/resources/virtual/two", handler.findVirtualHost("two.localdomain:8083"));
        assertEquals("src/test/resources/virtual/three-8081", handler.findVirtualHost("three.localdomain:8081"));
        assertEquals("src/test/resources/virtual/three-8082", handler.findVirtualHost("three.localdomain:8082"));

        assertNull(handler.findVirtualHost("three.localdomain:8083"));
        assertNull(handler.findVirtualHost("three.localdomain"));
        assertNull(handler.findVirtualHost("unknow.localdomain"));
        assertNull(handler.findVirtualHost("unknow.localdomain:8081"));
    }

    @Test
    public void testVirtualHostHome() throws Exception {
        VirtualHostHandler handler = new VirtualHostHandler();
        List<VirtualHost> hosts = new ArrayList<>();
        hosts.add(new VirtualHost()
                .domain("one.localdomain")
                .home("src/test/resources/virtual/one"));
        hosts.add(new VirtualHost()
                .domain("two.localdomain")
                .home("src/test/resources/virtual/two"));
        hosts.add(new VirtualHost()
                .domain("three.localdomain:8081")
                .home("src/test/resources/virtual/three-8081"));
        hosts.add(new VirtualHost()
                .domain("three.localdomain:8082")
                .home("src/test/resources/virtual/three-8082"));
        handler.setHosts(hosts);

        assertNotNull(handler.findLocalFile("one.localdomain", "/index.html"));
        assertEquals("src/test/resources/virtual/one/index.html", handler.findLocalFile("one.localdomain", "/index.html").toString());
        assertNotNull(handler.findLocalFile("one.localdomain", "/css/style.css"));
        assertEquals("src/test/resources/virtual/one/css/style.css", handler.findLocalFile("one.localdomain", "/css/style.css").toString());
    }

}
