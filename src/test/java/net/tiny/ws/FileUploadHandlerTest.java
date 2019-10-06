package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.*;


import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.client.SimpleClient;

public class FileUploadHandlerTest {

    static int port;
    static EmbeddedServer server;
    static TestFileUploadHandler uploader;

    @BeforeEach
    public void setUp() throws Exception {
        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        uploader = new TestFileUploadHandler();

        WebServiceHandler handler = uploader.path("/upload")
                .filters(Arrays.asList(logger, snap));
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

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
        server.awaitTermination();
    }

    @Test
    public void testSearchBytes() throws Exception {
        String boundary = "WebKitFormBoundaryKBRUiUWrIpW9wq2j";
        byte[] boundaryBytes = ("\r\n--" + boundary).getBytes(Charset.forName("UTF-8"));
        String contents =
                "\r\n" +
                "--WebKitFormBoundaryKBRUiUWrIpW9wq2j\r\n" +
                "Content-Disposition: form-data; name=\"textline\"\r\n" +
                "Content-type: text/plain;charset=UTF-8\r\n" +
                "\r\n" +
                "value of textfield here\r\n" +
                "--WebKitFormBoundaryKBRUiUWrIpW9wq2j\r\n" +
                "Content-Disposition: form-data; name=\"datafile\"; filename=\"test.txt\"\r\n" +
                "Content-type: application/octet-stream\r\n" +
                "\r\n" +
                "1234567890\r\n" +
                "abcdefghijk\r\n" +
                "--WebKitFormBoundaryKBRUiUWrIpW9wq2j--\r\n" +
                "";

        List<Integer> offsets = uploader.searchBytes(contents.getBytes(), boundaryBytes, 0, contents.length() -1);
        int startPart = offsets.get(0);
        int endPart = contents.length();
        assertEquals(3, offsets.size());
        assertEquals(0, startPart);
        assertEquals(371, endPart);

    }

    @Test
    public void testUploadFile() throws Exception {
        String boundary = "WebKitFormBoundaryKBRUiUWrIpW9wq2j";
        SimpleClient client = new SimpleClient.Builder().build();

        String contents =
                "\r\n" +
                "--WebKitFormBoundaryKBRUiUWrIpW9wq2j\r\n" +
                "Content-Disposition: form-data; name=\"textline\"\r\n" +
                "Content-type: text/plain;charset=UTF-8\r\n" +
                "\r\n" +
                "value of textfield here\r\n" +
                "--WebKitFormBoundaryKBRUiUWrIpW9wq2j\r\n" +
                "Content-Disposition: form-data; name=\"datafile\"; filename=\"test.txt\"\r\n" +
                "Content-type: application/octet-stream\r\n" +
                "\r\n" +
                "1234567890\r\n" +
                "abcdefghijk\r\n" +
                "--WebKitFormBoundaryKBRUiUWrIpW9wq2j--\r\n" +
                "";

        // Test POST Upload file
        client.request()
            .port(port)
            .path("/upload")
            .type("multipart/form-data;boundary=" + boundary)
            .doPost(contents.getBytes(),  callback -> {
                if(callback.success()) {
                    assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                    String res = new String(client.getContents());
                    System.out.println(res);
                } else {
                    Throwable err = callback.cause();
                    fail(err.getMessage());
                }
            });
        client.close();
        assertEquals(2, uploader.multiParts.size());
        FormDataHandler.MultiPart part = uploader.multiParts.get(0);
        assertEquals("textline", part.name);
        assertEquals("value of textfield here", part.value);
        assertNull(part.filename);

        part = uploader.multiParts.get(1);
        assertEquals("datafile", part.name);
        assertEquals("test.txt", part.filename);
        assertNull(part.value);
        assertEquals("1234567890\r\nabcdefghijk", new String(part.bytes));
    }


    static class TestFileUploadHandler extends FormDataHandler {
        List<MultiPart> multiParts;
        @Override
        public void handle(HttpExchange he, List<MultiPart> parts) throws IOException {
            if (null == parts) {
                he.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
                return;
            }
            multiParts = new ArrayList<>();
            multiParts.addAll(parts);
            for (MultiPart part : parts) {
                System.out.println(String.format("MultiParts: type=%s, name=%s, filename='%s' [%s]",
                        part.type, part.name, part.filename, part.value));
            }
            he.sendResponseHeaders(HttpURLConnection.HTTP_OK, -1);
        }

    }
}
