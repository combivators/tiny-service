package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.service.ServiceLocator;
import net.tiny.ws.client.SimpleClient;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.LogManager;


public class ControllerServiceTest {

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
        WebServiceHandler controller = handler
                .path("/v1/ctl")
                .filters(Arrays.asList(logger, snap));
        handler.setContext(context);

        builder = builder.handlers(Arrays.asList(controller));

        Thread task = new Thread(launcher);
        task.start();
        Thread.sleep(500L);
        assertTrue(launcher.isStarting());
    }

    @AfterEach
    public void tearDown() throws Exception {
        launcher.stop();
    }

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
        SimpleClient client = new SimpleClient.Builder()
                .build();

        client.doGet(new URL("http://localhost:" +port + "/v1/ctl/status"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertTrue(new String(client.getContents()).endsWith("running..."));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        client.close();
    }

}
