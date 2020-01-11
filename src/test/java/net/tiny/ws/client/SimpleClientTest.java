package net.tiny.ws.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.net.URL;


public class SimpleClientTest {

    @Test
    public void testRequestBuilder() throws Exception {
        SimpleClient client = new SimpleClient.Builder()
                .build();
        URL url = new URL("http://localhost:8080/v1/api/js?q=svg&t=min");
        assertEquals("/v1/api/js", url.getPath());
        assertEquals("q=svg&t=min", url.getQuery());
        SimpleClient.RequestBuilder requestBuilder = new SimpleClient.RequestBuilder(client, url);
        requestBuilder.host("127.0.0.1").port(80).path("/v1/api/js");
    }
}
