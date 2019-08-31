package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.ws.client.SimpleClient;

public class LauncherTest {

//    @BeforeAll
//    public static void beforeAll() throws Exception {
//    	LogManager.getLogManager()
//        	.readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
//    }

    @Test
    public void testStartStop() throws Exception {
        Launcher launcher = new Launcher();
        EmbeddedServer.Builder builder = launcher.getBuilder();
        assertNotNull(builder);
        assertFalse(launcher.isStarting());

        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();
        WebServiceHandler health = new VoidHttpHandler()
                .path("/health")
                .filter(logger);
        WebServiceHandler controller = new ControllableHandler()
                .path("/v1/ctl")
                .filters(Arrays.asList(logger, snap));

        builder = builder.handlers(Arrays.asList(controller, health));

        Thread task = new Thread(launcher);
        task.start();
        Thread.sleep(2000L);
        assertTrue(launcher.isStarting());


        SimpleClient client = new SimpleClient.Builder()
                .keepAlive(true)
                .build();

        byte[] contents = client.doGet(new URL("http://localhost:8080/v1/ctl/status"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);

            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });
        assertEquals("running", new String(contents));

        client.doGet(new URL("http://localhost:8080/health"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        contents = client.doGet(new URL("http://localhost:8080/v1/ctl/stop"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });
        assertEquals("stopping", new String(contents));

        client.close();

        Thread.sleep(3000L);
        assertFalse(launcher.isStarting());
    }

}
