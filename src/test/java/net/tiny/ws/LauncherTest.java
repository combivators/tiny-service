package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.service.ServiceLocator;
import net.tiny.ws.client.SimpleClient;

public class LauncherTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    static int port;
    static Launcher launcher;

    @BeforeEach
    public void setUp() throws Exception {
        ServiceLocator context = new ServiceLocator();
        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        launcher = new Launcher();
        context.bind("launcher", launcher, true);

        EmbeddedServer.Builder builder = launcher.getBuilder()
                .random();
        port = builder.port;

        ControllableHandler handler = new ControllableHandler();
        WebServiceHandler health = new VoidHttpHandler()
                .path("/health")
                .filter(logger);
        WebServiceHandler controller = handler
                .path("/v1/ctl")
                .filters(Arrays.asList(logger, snap));
        handler.setContext(context);

        builder = builder.handlers(Arrays.asList(controller, health));

        Thread task = new Thread(launcher);
        task.start();
        Thread.sleep(500L);
        assertTrue(launcher.isStarting());
    }

    @AfterEach
    public void tearDown() throws Exception {

        SimpleClient client = new SimpleClient.Builder()
                .build();
        byte[] contents = client.doGet(new URL("http://localhost:" + port + "/v1/ctl/stop"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });
        System.out.println(new String(contents));

        client.close();
    }


    @Test
    public void testStartStop() throws Exception {

        SimpleClient client = new SimpleClient.Builder()
                .keepAlive(true)
                .build();

        byte[] contents = client.doGet(new URL("http://localhost:" + port + "/v1/ctl/status"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);

            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });
        String response  =  new String(contents);
        System.out.println(response);
        assertTrue(response.endsWith("running..."));

        client.doGet(new URL("http://localhost:" + port + "/health"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        client.close();
    }

}
