package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import net.tiny.ws.client.SimpleClient;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;


public class ControllerServiceTest {

    @Test
    public void testIsVaildRequest() throws Exception {
        assertFalse(ControllableHandler.isVaildRequest("shutdown"));
        assertTrue(ControllableHandler.isVaildRequest("status"));
        assertFalse(ControllableHandler.isVaildRequest("status12"));
        assertTrue(ControllableHandler.isVaildRequest("start&abc"));
        assertFalse(ControllableHandler.isVaildRequest("start&"));
        assertTrue(ControllableHandler.isVaildRequest("stop&ABZ"));
        assertTrue(ControllableHandler.isVaildRequest("suspend&123"));
        assertTrue(ControllableHandler.isVaildRequest("resume&abc&123"));
    }


    @Test
    public void testController() throws Exception {
        final int port = 8080;
        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        WebServiceHandler controller = new ControllableHandler()
                .path("/v1/ctl")
                .filters(Arrays.asList(logger, snap));

        WebServiceHandler health = new VoidHttpHandler()
                .path("/healthcheck")
                .filter(logger);

        EmbeddedServer server = new EmbeddedServer.Builder()
                .port(port)
                .handlers(Arrays.asList(controller, health))
                .build();
        server.listen(callback -> {
            if(callback.success()) {
                System.out.println("Server listen on port: " + port);
            } else {
                callback.cause().printStackTrace();
            }
        });

        SimpleClient client = new SimpleClient.Builder()
        		.build();

        client.doGet(new URL("http://localhost:" +port + "/v1/ctl/status"), callback -> {
            if(callback.success()) {
            	assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
            	assertEquals("running", new String(client.getContents()));
            } else {
            	Throwable err = callback.cause();
            	fail(err.getMessage());
            }
        });

        client.close();

        server.stop();
        server.awaitTermination();
    }

}
