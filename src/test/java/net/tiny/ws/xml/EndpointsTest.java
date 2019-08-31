package net.tiny.ws.xml;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXB;

public class EndpointsTest {

    @Test
    public void testToXML() throws Exception {
        Endpoints endpoints = new Endpoints();
        Endpoints.Endpoint ep = new Endpoints.Endpoint();
        ep.name = "AccountDetailsServiceEndPoint";
        ep.service = "{accounts}AccountDetailsService";
        ep.port = "{accounts}AccountDetailsPort";
        ep.implementation = "com.mg.ws.impl.AccountDetailsServiceImpl";
        ep.urlPattern = "/details";
        ep.wsdl = "WEB-INF/wsdl/accounts.wsdl";
        endpoints.addEndpoint(ep);

        StringWriter writer = new StringWriter();
        JAXB.marshal(endpoints, writer);

        String xml = writer.toString();
        System.out.println(xml);
        System.out.println();

        ByteArrayInputStream  bis = new ByteArrayInputStream(xml.getBytes());
        Endpoints other = JAXB.unmarshal(bis, Endpoints.class);
        assertNotNull(other);
        System.out.println(other.toString());

        endpoints = JAXB.unmarshal(new File("src/test/resources/jaxws.xml"), Endpoints.class);
        ep = endpoints.getEndpoints().get(0);
        assertEquals("NamingServiceWS", ep.name);
        assertEquals("{http://ws.eac.com/}NamingService", ep.service);
        assertEquals("{http://ws.eac.com/}NamingServicePort", ep.port);
        assertEquals("com.eac.ws.WebNamingService", ep.implementation);
        assertEquals("/ns.ws", ep.urlPattern);

        assertEquals("http://ws.eac.com/", ep.getTargetNamespace());
        assertEquals("NamingService", ep.getSimpleService());
        assertEquals("NamingServicePort", ep.getSimplePort());
        ep.setContents("<wsdl/>".getBytes());
        ep.setServletName("hello");

        writer = new StringWriter();
        JAXB.marshal(endpoints, writer);

        xml = writer.toString();
        System.out.println(xml);
    }

    @Test
    public void testNamingspace() throws Exception {
        String regex = "^[{][\\S][^}]+[}][\\S][^}]+";
        assertTrue(Pattern.matches(regex, "{http://abc.com/}NamingService"));

        assertFalse(Pattern.matches(regex, ""));
        assertFalse(Pattern.matches(regex, "http://abc.com/}NamingService"));
        assertFalse(Pattern.matches(regex, "{http://abc.com/NamingService"));
        assertFalse(Pattern.matches(regex, "NamingService"));
        assertFalse(Pattern.matches(regex, "{}NamingService"));
        assertFalse(Pattern.matches(regex, "{abc}Naming}Service"));

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher("{http://abc.com/}NamingService");
        while(matcher.find()) {
            int num = matcher.groupCount();
            String[] args = new String[num];
            for(int i=1; i<=num; i++) {
                args[i-1] = matcher.group(i);
            }
            System.out.println("num: " + num);
        }
    }
}
