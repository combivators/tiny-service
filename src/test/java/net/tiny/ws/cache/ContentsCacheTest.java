package net.tiny.ws.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class ContentsCacheTest {

    @Test
    public void testGetLocalFile() throws Exception {
        Path cachePath = Paths.get("src/test/resources/cache");
        if (!Files.exists(cachePath)) {
            cachePath = Files.createDirectory(cachePath);
        }
        assertTrue(Files.isDirectory(cachePath));

        for (int i=1; i<=5; i++) {
            final String name = "src/test/resources/cache/file" + i;
            Path p = Paths.get(name);
            if (!Files.exists(p)) {
                Files.createFile(p);
                Files.write(p, Arrays.asList(name),
                        Charset.forName("UTF-8"), StandardOpenOption.WRITE);
            }
        }

        // Cache max 3 files
        ContentsCache cache = new ContentsCache(3);
        assertTrue(cache.get(new File("src/test/resources/cache/file1").toURI().toURL()).length > 0);
        assertTrue(cache.get(new File("src/test/resources/cache/file2").toURI().toURL()).length > 0);
        assertTrue(cache.get(new File("src/test/resources/cache/file3").toURI().toURL()).length > 0);
        assertTrue(cache.get(new File("src/test/resources/cache/file4").toURI().toURL()).length > 0);
        assertTrue(cache.get(new File("src/test/resources/cache/file5").toURI().toURL()).length > 0);
        assertEquals("Cache(3/3)", cache.toString());

        try {
            cache.get(new File("src/test/resources/cache/file6").toURI().toURL());
        } catch(Exception e) {
            assertTrue(e instanceof IOException);
            assertTrue(e.getMessage().contains("file6"));
        }

        cache.clear();
    }

    @Test
    public void testGetResources() throws Exception {
        Path cachePath = Paths.get("src/test/resources/cache");
        if (!Files.exists(cachePath)) {
            cachePath = Files.createDirectory(cachePath);
        }
        assertTrue(Files.isDirectory(cachePath));

        for (int i=1; i<=5; i++) {
            final String name = "src/test/resources/cache/file" + i;
            Path p = Paths.get(name);
            if (!Files.exists(p)) {
                Files.createFile(p);
                Files.write(p, Arrays.asList(name),
                        Charset.forName("UTF-8"), StandardOpenOption.WRITE);
            }
        }

        ClassLoader loader = ContentsCache.class.getClassLoader();
        ;
        // Cache max 3 files
        ContentsCache cache = new ContentsCache(3);
        assertTrue(cache.get(loader.getResource("cache/file1").toURI().toURL()).length > 0);
        assertTrue(cache.get(loader.getResource("cache/file2").toURI().toURL()).length > 0);
        assertTrue(cache.get(loader.getResource("cache/file3").toURI().toURL()).length > 0);
        assertTrue(cache.get(loader.getResource("cache/file4").toURI().toURL()).length > 0);
        assertTrue(cache.get(loader.getResource("cache/file5").toURI().toURL()).length > 0);
        assertEquals("Cache(3/3)", cache.toString());

        try {
            cache.get(loader.getResource("cache/file6").toURI().toURL());
        } catch(Exception e) {
            assertTrue(e instanceof NullPointerException);
        }

        cache.clear();
    }
}
