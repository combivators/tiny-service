package net.tiny.ws.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class CacheTest {

    @Test
    public void testGet() throws Throwable {
        assertEquals("value001", String.format("value%03d", Integer.parseInt("1")));

        BarakbCache<String, byte[]> cache =
                new BarakbCache<>(key -> readContents(key), 10);
        cache.setRemoveableException(RuntimeException.class);

        for(int i=0; i<8; i++) {
            assertTrue(cache.get(String.valueOf(i)).length > 0);
        }
        assertEquals(cache.size(), 8);
        assertEquals("Cache(8/10)", cache.toString());

        for(int i=0; i<20; i++) {
            try {
                assertTrue(cache.get(String.valueOf(i)).length > 0);
            } catch (Exception e) {
                assertTrue(e instanceof IllegalArgumentException);
                assertEquals("Is a illegal argument '15'.", e.getMessage());
            }
        }
        assertEquals("Cache(10/10)", cache.toString());

        cache.clear();
    }

    private byte[] readContents(String key) {
        if("15".equals(key)) {
            throw new IllegalArgumentException("Is a illegal argument '" + key + "'.");
        }
        return String.format("value%03d", Integer.parseInt(key)).getBytes();
    }
}
