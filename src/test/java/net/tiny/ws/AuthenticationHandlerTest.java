package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import net.tiny.config.JsonParser;
import net.tiny.ws.auth.AccountService;
import net.tiny.ws.auth.CallbackContext;
import net.tiny.ws.client.SimpleClient;

public class AuthenticationHandlerTest {

    static int port = 8080;
    static EmbeddedServer server;

    @BeforeAll
    public static void setUp() throws Exception {
        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        CallbackContext.setConfig("src/test/resources/jaas/jaas.conf");

        final AccountService service = new AccountService();
        service.setPath("src/test/resources/jaas/passwd");

        final ServerRepository repository = new ServerRepository();
        repository.setSessionTimeout(3000L);
        final AuthenticationHandler authentication = new AuthenticationHandler();
        authentication.setRealm("default");
        authentication.setService(service);
        authentication.setServerRepository(repository);
        final WebServiceHandler handler = authentication
        		.path("/account")
        		.filters(Arrays.asList(logger, snap));

        server = new EmbeddedServer.Builder()
                .port(port)
                .handlers(Arrays.asList(handler))
                .build();
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

    @SuppressWarnings("unchecked")
	@Test
    public void testBasicLoginLogout() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
        		.credentials("admin", "password")
                .keepAlive(true)
                .build();

        String token;
        client.doGet(new URL("http://localhost:" + port +"/account/login"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
                assertEquals("no-cache, no-store", client.getHeader("Cache-control"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        token = client.getCookie(Constants.COOKIE_AUTHTOKEN);
        assertNotNull(token);
        String json = new String(client.getContents());
        System.out.println(json);
        Map<String, Object> map = JsonParser.unmarshal(json, Map.class);
        assertTrue(map.containsKey("token"));
        assertEquals(map.get("token"), token);

        client.doGet(new URL("http://localhost:" + port +"/account/logout"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                final String tokenCookie = client.getHeader("Set-cookie");
                assertTrue(tokenCookie.contains(token));
                assertTrue(tokenCookie.contains("max-age=0"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        assertNull(client.getCookie(Constants.COOKIE_AUTHTOKEN));

        client.close();
    }


    @SuppressWarnings("unchecked")
	@Test
    public void testUrlPathLoginLogout() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
                .keepAlive(true)
                .build();

        String token;
        client.doGet(new URL("http://localhost:" + port +"/account/login/admin/password"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
                assertEquals("no-cache, no-store", client.getHeader("Cache-control"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        token = client.getCookie(Constants.COOKIE_AUTHTOKEN);
        assertNotNull(token);
        String json = new String(client.getContents());
        System.out.println(json);
        Map<String, Object> map = JsonParser.unmarshal(json, Map.class);
        assertTrue(map.containsKey("token"));
        assertEquals(map.get("token"), token);

        client.doGet(new URL("http://localhost:" + port +"/account/logout"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                final String tokenCookie = client.getHeader("Set-cookie");
                assertTrue(tokenCookie.contains(token));
                assertTrue(tokenCookie.contains("max-age=0"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        assertNull(client.getCookie(Constants.COOKIE_AUTHTOKEN));

        client.close();
    }

    @SuppressWarnings("unchecked")
	@Test
    public void testQueryLoginLogout() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
                .keepAlive(true)
                .build();

        String token;
        client.doGet(new URL("http://localhost:" + port +"/account/login?u=admin&p=password"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("application/json; charset=utf-8", client.getHeader("Content-Type"));
                assertEquals("no-cache, no-store", client.getHeader("Cache-control"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        token = client.getCookie(Constants.COOKIE_AUTHTOKEN);
        assertNotNull(token);
        String json = new String(client.getContents());
        System.out.println(json);
        Map<String, Object> map = JsonParser.unmarshal(json, Map.class);
        assertTrue(map.containsKey("token"));
        assertEquals(map.get("token"), token);

        client.doGet(new URL("http://localhost:" + port +"/account/logout"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                final String tokenCookie = client.getHeader("Set-cookie");
                assertTrue(tokenCookie.contains(token));
                assertTrue(tokenCookie.contains("max-age=0"));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        assertNull(client.getCookie(Constants.COOKIE_AUTHTOKEN));

        client.close();
    }


	@Test
    public void testLoginFailed() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
        		.credentials("admin", "badpassword")
                .keepAlive(true)
                .build();


        client.doGet(new URL("http://localhost:" + port +"/account/login"), callback -> {
            if(callback.fail()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
            } else {
            	fail("Should be fail.");
            }
        });

        client.close();
    }

	@Test
    public void testUnknowAccount() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
        		.credentials("adminxxx", "password")
                .keepAlive(true)
                .build();


        client.doGet(new URL("http://localhost:" + port +"/account/login"), callback -> {
            if(callback.fail()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
            } else {
            	fail("Unknow Account");
            }
        });

        client.close();
    }

	@Test
    public void testAccountExpired() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
        		.credentials("Wayne", "password0")
                .keepAlive(true)
                .build();


        client.doGet(new URL("http://localhost:" + port +"/account/login"), callback -> {
            if(callback.fail()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
            } else {
            	fail("Account Expired.");
            }
        });

        client.close();
    }

	@Test
    public void testAccountLocked() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
        		.credentials("Jone", "password")
                .keepAlive(true)
                .build();


        client.doGet(new URL("http://localhost:" + port +"/account/login"), callback -> {
            if(callback.fail()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
            } else {
            	fail("Account Locked.");
            }
        });

        client.close();
    }

	@Test
    public void testLogoutFailed() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
                .build();

        client.doGet(new URL("http://localhost:" + port +"/account/logout"), callback -> {
            if(callback.fail()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_UNAUTHORIZED);
            } else {
            	fail("Account not login.");
            }
        });

        client.close();
    }

	@Test
    public void testInvalidToken() throws Exception {
        final SimpleClient client = new SimpleClient.Builder()
                .build();

        client.request(new URL("http://localhost:" + port +"/account/logout"))
        	.header("Cookie", "AT=HFtDaOgmtxSfHub0m/P2pR8UKIxKzWGJLKFIDKAyyFfh6FEsO6D77imu6ymLI0nNbMTxOp5tzQZ6qqsiAXR5vx")
        	.doGet(callback -> {
	            if(callback.fail()) {
	                assertEquals(client.getStatus(), HttpURLConnection.HTTP_FORBIDDEN);
	            } else {
	            	fail("Invalid token.");
	            }
        });

        client.close();
    }
}
