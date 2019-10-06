package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.logging.LogManager;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.ws.client.SimpleClient;

public class EndpointServiceTest {


    static int port;
    static EmbeddedServer server;

    @BeforeAll
    public static void setUp() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));

        AccessLogger logger = new AccessLogger();
        SnapFilter snap = new SnapFilter();

        final CalculatorServer endpoint = new CalculatorServer();
        final WebServiceHandler handler = endpoint
                .path("/endpoint")
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

    @AfterAll
    public static void tearDown() throws Exception {
        server.close();
    }


    @Test
    public void testAccessEndpoint() throws Exception {
        SimpleClient client = new SimpleClient.Builder()
                .keepAlive(true)
                .build();

        client.doGet(new URL("http://localhost:" + port +"/endpoint?wsdl"), callback -> {
            if(callback.success()) {
                assertEquals(client.getStatus(), HttpURLConnection.HTTP_OK);
                assertEquals("text/xml;charset=utf-8", client.getHeader("Content-Type"));
                assertEquals(4129, client.getContents().length);
                //System.out.print(new String(client.getContents()));
                final String contets = new String(client.getContents());
                assertTrue(contets.contains("<port name=\"CalculatorServicePort\" binding=\"tns:CalculatorServicePortBinding\">"));
                String address = String.format("<soap:address location=\"http://localhost:%d/endpoint\"></soap:address>", port);
                assertTrue(contets.contains(address));
            } else {
                Throwable err = callback.cause();
                fail(err.getMessage());
            }
        });

        client.close();

        CalculatorService calculator = new CalculatorClient(new URL("http://localhost:" + port +"/endpoint")).get();
        assertEquals(579, calculator.sum(123, 456));
        assertEquals(333, calculator.diff(456, 123));
    }


    @WebService(name = "NS", targetNamespace = "http://ws.tiny.net/")
    @SOAPBinding(style = SOAPBinding.Style.RPC)
    public static interface CalculatorService {
        @WebMethod
        int sum(int a, int b);

        @WebMethod
        int diff(int a, int b);

        @WebMethod
        int multiply(int a, int b);

        @WebMethod
        int divide(int a, int b);
    }

    @WebService(serviceName = "CalculatorService",
            portName = "CalculatorServicePort",
            endpointInterface = "net.tiny.ws.EndpointServiceTest$CalculatorService")
    public static class CalculatorServer extends AbstractWebService implements CalculatorService {
        @Override
        public int sum(int a, int b) {
            return a+b;
        }

        @Override
        public int diff(int a, int b) {
            return a-b;
        }

        @Override
        public int multiply(int a, int b) {
            return a*b;
        }

        @Override
        public int divide(int a, int b) {
            return a/b;
        }
    }

    public static class CalculatorClient implements Supplier<CalculatorService> {

        final CalculatorService calculator;

        CalculatorClient(URL url) {
            QName qname = new QName("http://ws.tiny.net/", "CalculatorService");
            Service service = Service.create(url, qname);
            calculator = service.getPort(CalculatorService.class);
        }

        @Override
        public CalculatorService get() {
            return calculator;
        }
    }
}
