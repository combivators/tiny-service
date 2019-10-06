package net.tiny.ws.client;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.config.JsonParser;
import net.tiny.ws.AccessLogger;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.SnapFilter;
import net.tiny.ws.WebServiceHandler;

public class OptionsServerTest {

    static String BROWSER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager().readConfiguration(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    @BeforeEach
    public void setUp() throws Exception {
        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        WebServiceHandler handler = new PostPutOptionHandler()
                .filters(Arrays.asList(logger, snap));

        server = new EmbeddedServer.Builder()
                .random()
                .handler("/opt", handler)
                .build();
        port = server.port();
        server.listen(callback -> {
            if(callback.success()) {
                System.out.println("Server listen on port: " + port);
            } else {
                callback.cause().printStackTrace();
            }
        });
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
        server.awaitTermination();
    }

    @Test
    public void testPutPostEntity() throws Exception {

        SimpleClient client = new SimpleClient.Builder()
                .userAgent(BROWSER_AGENT)
                .keepAlive(true)
                .build();

        // Test PUT
        client.request()
            .port(port)
            .path("/opt")
            .type(SimpleClient.MIME_TYPE_JSON)
            .accept("application/json")
            .doPut("{\"email\":\"user@example.com\"}".getBytes(),  callback -> {
                if(callback.success()) {
                    assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                    assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
                    String res = new String(client.getContents());
                    assertTrue(res.length() > 30);
                    System.out.println(res);
                } else {
                    Throwable err = callback.cause();
                    fail(err.getMessage());
                }
            });

        // Test POST
        PostPutOptionHandler.DummyEmail mail = new PostPutOptionHandler.DummyEmail();
        mail.email = "user@example.com";

        PostPutOptionHandler.DummyUser user =
                client.doPost("http://localhost:" + port +"/opt", mail, PostPutOptionHandler.DummyUser.class);
        assertNotNull(user);
        assertEquals("user", user.name);

        String req = JsonParser.marshal(mail);
        byte[] res = client.doPost(new URL("http://localhost:" + port +"/opt"), req.getBytes(), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        user = JsonParser.unmarshal(new String(res), PostPutOptionHandler.DummyUser.class);
        assertNotNull(user);
        assertEquals("example.com", user.domain);

        client.close();
    }
}
