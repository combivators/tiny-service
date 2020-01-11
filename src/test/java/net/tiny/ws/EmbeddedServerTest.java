package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.Test;



public class EmbeddedServerTest {

    @Test
    public void testRandomPort() throws Exception {
        Integer port = ThreadLocalRandom.current().nextInt(8080, 8180);
        assertTrue( (port>=8080 && port <= 8180));
        EmbeddedServer.RandomPorts ports = new EmbeddedServer.RandomPorts(8080, 8180);

        List<Integer> list = new ArrayList<>();
        for (int i=0; i<100; i++) {
            Integer p = ports.next();
            if (list.contains(p)) {
                fail("Duplication port " + p);
                System.out.println("Duplication port " + p);
            } else {
                list.add(p);
            }
        }
    }

    @Test
    public void testAvailablePort() throws Exception {
        final int port = 8888;
        assertTrue(EmbeddedServer.available(8888));
        assertTrue(EmbeddedServer.available(8000));
        assertTrue(EmbeddedServer.available(9999));
        assertThrows(IllegalArgumentException.class, () -> {
            EmbeddedServer.available(79);
          });
        assertThrows(IllegalArgumentException.class, () -> {
            EmbeddedServer.available(10000);
          });

        EmbeddedServer server = new EmbeddedServer.Builder()
                .handler("/health", new VoidHttpHandler())
                .port(port)
                .build();

        server.listen(callback -> {
            if(callback.success()) {
                System.out.println("Server listen on port: " + port);
            } else {
                callback.cause().printStackTrace();
            }
        });

        assertFalse(EmbeddedServer.available(8888));

        server.stop();
        server.awaitTermination();
    }
}
