package net.tiny.ws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.config.PropertiesSupport;
import net.tiny.config.YamlLoader;

public class ApplicationConfigurationTest {


    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager().readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }


    @Test
    public void testSslYaml() throws Exception {
        System.out.println("========== testSslYaml =========");
        final String yaml = new String(Files.readAllBytes(Paths.get("src/test/resources/application-ssl.yml")));
        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        //properties.list(System.out);
        assertEquals(64, properties.size());
        assertEquals("${launcher.http},${launcher.https}", properties.getProperty("main"));
        assertEquals("${handler.sys},${handler.health}", properties.getProperty("launcher.http.builder.handlers"));
        assertEquals("${handler.home}", properties.getProperty("launcher.https.builder.handlers"));
    }

    @Test
    public void testTestYaml() throws Exception {
        System.out.println("========== testTestYaml =========");
        final String yaml = new String(Files.readAllBytes(Paths.get("src/test/resources/application-dev.yml")));
        Properties properties = YamlLoader.load(new StringReader(yaml), new PropertiesSupport.Monitor());
        //properties.list(System.out);
        assertEquals(65, properties.size());
        assertEquals("${launcher.http},${launcher.https}", properties.getProperty("main"));
        assertEquals("${handler.sys},${handler.home}", properties.getProperty("launcher.http.builder.handlers"));
        assertEquals("${handler.health}", properties.getProperty("launcher.https.builder.handlers"));
    }
}
