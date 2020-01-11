package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.tiny.benchmark.Benchmarker;
import net.tiny.ws.EmbeddedServer;
import net.tiny.ws.ResourceHttpHandler;
import net.tiny.ws.client.SimpleClient;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.LogManager;


public class ResourceServerTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

	static String BROWSER_AGENT = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
    static int port;
    static EmbeddedServer server;

    @BeforeEach
    public void setUp() throws Exception {
        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        List<String> paths = Arrays.asList(
                "img:src/test/resources/home/img",
                "css:src/test/resources/home/css",
                "js:src/test/resources/home/js",
                "icon:src/test/resources/home/icon"
                );
        WebServiceHandler resources = new ResourceHttpHandler()
                .setPaths(paths)
                .path("/")
                .filters(Arrays.asList(logger, snap));
        WebServiceHandler health = new VoidHttpHandler()
                .path("/health")
                .filter(logger);
        WebServiceHandler json = new TestJsonHandler()
                .path("/json")
                .filters(Arrays.asList(logger, snap));
        server = new EmbeddedServer.Builder()
                .random()
                .handlers(Arrays.asList(resources, health, json))
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
    public void testSimpleClientGetMethod() throws Exception {
        SimpleClient client = new SimpleClient.Builder()
                .userAgent(BROWSER_AGENT)
                .keepAlive(true)
                .build();

        client.request()
            .port(port)
            .path("/health")
            .doGet(callback -> {
                if(callback.success()) {
                    assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                } else {
                    Throwable err = callback.cause();
                    fail(err.getMessage());
                }
            });

        client.close();
    }

    @Test
    public void testGetResources() throws Exception {

        SimpleClient client = new SimpleClient.Builder()
                .userAgent(BROWSER_AGENT)
                .keepAlive(true)
                .build();

        client.request()
            .port(port)
            .path("/icon/favicon.ico")
            .doGet(callback -> {
                if(callback.success()) {
                    assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                    assertEquals("image/vnd.microsoft.icon", client.getHeader("Content-Type"));
                    assertEquals(5686, client.getContents().length);
                } else {
                    Throwable err = callback.cause();
                    fail(err.getMessage());
                }
            });

        client.doGet(new URL("http://localhost:" + port +"/css/style.css"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("text/css; charset=utf-8", client.getHeader("Content-Type"));
                assertEquals(420, client.getContents().length);
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        client.request(new URL("http://localhost:" + port +"/css/style.css"))
            .doGet(callback -> {
                if(callback.success()) {
                    assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                } else {
                    Throwable err = callback.cause();
                    fail(err.getMessage());
                }
            });

        client.close();
    }

    @Test
    public void testLastModified() throws Exception {
        SimpleClient client = new SimpleClient.Builder()
                .userAgent(BROWSER_AGENT)
                .keepAlive(true)
                .build();

        client.doGet(new URL("http://localhost:" + port +"/css/style.css"), callback -> {
            if(callback.success()) {
                assertEquals(HttpURLConnection.HTTP_OK, client.getStatus());
                assertEquals("text/css; charset=utf-8", client.getHeader("Content-Type"));
                assertTrue(!client.getHeader("Last-modified").isEmpty());
                assertEquals(420, client.getContents().length);
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        Date lastModified = HttpDateFormat.parse(client.getHeader("Last-modified"));
        assertNotNull(lastModified);

        client.request().port(port).path("/css/style.css")
            .header("If-Modified-Since", HttpDateFormat.format(lastModified))
            .doGet(callback -> {
                if(callback.success()) {
                    assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, client.getStatus());
                    assertEquals("Keep-Alive", client.getHeader("Connection"));
                } else {
                    Throwable err = callback.cause();
                    fail(err.getMessage());
                }
            });

        client.close();

    }

    @Test
    public void testJson() throws Exception {
        SimpleClient client = new SimpleClient.Builder().build();
        String json = client.doGet("http://localhost:" + port +"/json");
        assertEquals("['hello world!']", json);
        client.close();
    }

    @Test
    public void testBenchmarkRequest() throws Exception {

        SimpleClient client = new SimpleClient.Builder()
                .keepAlive(true)
                .build();

        client.request(new URL("http://localhost:" + port + "/health"))
        .doGet(callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        Benchmarker bench = new Benchmarker();
        bench.start(1000L, 100L);
        while (bench.loop()) {
            bench.trace(System.out);
            client.doGet("http://localhost:" + port +"/health");
        }
        bench.stop();

        //Summary ETA:2s 689ms 299482ns MIPS:0.001 1.626ms/per min:0.437K/s max:0.719K/s avg:0.608K/s mean:0.601K/s count:1000 lost:2ms 111242ns
        bench.metric(System.out);
        client.close();
    }
}
