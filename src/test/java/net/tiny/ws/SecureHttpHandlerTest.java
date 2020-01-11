package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.config.JsonParser;
import net.tiny.ws.client.SimpleClient;


public class SecureHttpHandlerTest {

    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void setUp() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));

        AccessLogger logger = new AccessLogger();
        ParameterFilter parameter = new ParameterFilter();
        SnapFilter snap = new SnapFilter();

        final ServerRepository repository = new ServerRepository();
        repository.setSessionTimeout(3000L);
        final SecureHttpHandler secure = new SecureHttpHandler();
        secure.setServerRepository(repository);
        final WebServiceHandler handler = secure
                .path("/secure")
                .filters(Arrays.asList(parameter, logger, snap));

        server = new EmbeddedServer.Builder()
                .random()
                .handlers(Arrays.asList(handler))
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

    @AfterAll
    public static void tearDown() throws Exception {
        server.close();
    }

    @Test
    public void testGetSecurePublicKey() throws Exception {
        SimpleClient client = new SimpleClient.Builder()
                .keepAlive(true)
                .build();

        String sessionId;
        client.doGet(new URL("http://localhost:" + port +"/secure/public"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
                assertEquals("Mon, 26 Jul 1997 05:00:00 GMT", client.getHeader("Expires"));
                assertEquals("no-cache, no-store, must-revalidate", client.getHeader("Cache-control"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        sessionId = client.getCookie(Constants.COOKIE_SESSION);
        assertNotNull(sessionId);
        String json = new String(client.getContents());
        System.out.println(json);
        Map<?, ?> map = JsonParser.unmarshal(json, Map.class);
        assertTrue(map.containsKey("publicKey"));
        assertTrue(map.containsKey("modulus"));
        assertTrue(map.containsKey("exponent"));

        client.doGet(new URL("http://localhost:" + port +"/secure/public"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
                assertEquals("Mon, 26 Jul 1997 05:00:00 GMT", client.getHeader("Expires"));
                assertEquals("no-cache, no-store, must-revalidate", client.getHeader("Cache-control"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        String otherId = client.getCookie(Constants.COOKIE_SESSION);
        assertEquals(sessionId, otherId);

        client.close();
    }
}
