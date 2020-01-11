package net.tiny.cache;


import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;


public class LurCacheTest {

    @Test
    public void testLurMemoryCache() throws Exception {
        MemoryCache<String, String> cache = new MemoryCache<String, String>(20) {
            @Override
            protected int getSize(String value) {
                return value.length();
            }
        };
        assertEquals(cache.limit(), 20);
        assertEquals(cache.capacity(), 0);

        assertTrue(cache.put("1", "value01"));
        assertTrue(cache.put("2", "value02"));
        assertNotNull(cache.get("1"));
        assertEquals(cache.get("1"), "value01");
        assertEquals(cache.get("2"), "value02");

        // 1: hits2 2: hits1
        assertEquals(cache.capacity(), 14);
        assertTrue(cache.put("3", "value-03"));
        assertEquals(cache.capacity(), 15);
        assertNotNull(cache.get("1"));
        assertNull(cache.get("2"));
        assertNotNull(cache.get("3"));

        assertEquals(cache.toString(), "Cache(2): 20(0.75%) Age:30s");

        cache.clear();
        assertEquals(cache.capacity(), 0);
        assertNull(cache.get("1"));
        assertNull(cache.get("3"));
    }

    @Test
    public void testLurCacheMaxAge() throws Exception {
        MemoryCache<String, String> cache = new MemoryCache<String, String>(40, 1L) {
            @Override
            protected int getSize(String value) {
                return value.length();
            }
        };

        assertTrue(cache.put("1", "value01"));
        assertTrue(cache.put("2", "value02"));
        assertTrue(cache.put("3", "value03"));
        assertTrue(cache.put("4", "value04"));
        assertTrue(cache.put("5", "value05"));
        assertEquals(cache.capacity(), 35);
        assertEquals(cache.toString(), "Cache(5): 40(0.88%) Age:1s");
        Thread.sleep(100L);
        assertEquals(cache.get("4"), "value04");
        Thread.sleep(100L);
        assertEquals(cache.get("3"), "value03");
        Thread.sleep(100L);
        assertEquals(cache.get("2"), "value02");
        Thread.sleep(100L);
        assertEquals(cache.get("1"), "value01");
        Thread.sleep(100L);
        assertTrue(cache.put("6", "value06"));
        assertNull(cache.get("5"));
        Thread.sleep(800L);
        assertEquals(cache.get("4"), "value04");
        assertNull(cache.get("4"));
        assertEquals(cache.get("6"), "value06");

        cache.clear();
    }


    @Test
    public void testLurDiskCache() throws Exception {
        String tempPath = System.getProperty("java.io.tmpdir");
        DiskCache<String, String> cache = new DiskCache<String, String>(tempPath, 20, 60L*60L*12L) {

            protected int bufferSize = 1024;
            @Override
            protected int getSize(String value) {
                return value.length();
            }
            @Override
            protected void save(File file, String value) throws IOException {
                System.out.println("save " + file.getAbsolutePath() + "  " + value);
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
                try {
                    os.write(value.getBytes());
                    os.flush();
                } finally {
                    closeSilently(os);
                }

            }

            @Override
            protected String load(File file) throws IOException {
                System.out.println("load " + file.getAbsolutePath());
                InputStream is = new BufferedInputStream(new FileInputStream(file), bufferSize);
                try {
                    byte[] buffer = new byte[bufferSize];
                    int size = is.read(buffer);
                    return new String(buffer, 0, size);
                } finally {
                    closeSilently(is);
                }
            }
        };

        assertEquals(cache.limit(), 20);
        assertEquals(cache.capacity(), 0);

        assertTrue(cache.put("1", "value01"));
        assertTrue(cache.put("2", "value02"));
        assertNotNull(cache.get("1"));
        assertEquals(cache.get("1"), "value01");
        assertEquals(cache.get("2"), "value02");

        // 1: hits2 2: hits1
        assertEquals(cache.capacity(), 14);
        assertTrue(cache.put("3", "value-03"));
        assertEquals(cache.capacity(), 15);
        assertNotNull(cache.get("1"));
        assertNull(cache.get("2"));
        assertNotNull(cache.get("3"));

        assertEquals(cache.toString(), "Cache(2): 20(0.75%) Age:43200s");

        System.out.println("Put-4 " + cache.put("4", "value04"));
        System.out.println("Put-5 " + cache.put("5", "value05"));
        System.out.println("Put-6 " + cache.put("6", "value06"));
        //assertThat(cache.put("4", "value04"), equalTo(Boolean.TRUE));
        //assertThat(cache.put("5", "value05"), equalTo(Boolean.TRUE));
        //assertThat(cache.put("6", "value06"), equalTo(Boolean.FALSE));

        cache.clear();
        assertEquals(cache.capacity(), 0);
        assertNull(cache.get("1"));
        assertNull(cache.get("3"));
    }


    @Test
    public void testNoneLurDiskCache() throws Exception {
        String tempPath = System.getProperty("java.io.tmpdir");
        DiskCache<String, String> cache = new DiskCache<String, String>(tempPath, 20, 60L*60L*12L) {

            protected int bufferSize = 1024;
            @Override
            protected int getSize(String value) {
                return value.length();
            }
            @Override
            protected void save(File file, String value) throws IOException {
                System.out.println("save " + file.getAbsolutePath() + "  " + value);
                OutputStream os = new BufferedOutputStream(new FileOutputStream(file), bufferSize);
                try {
                    os.write(value.getBytes());
                    os.flush();
                } finally {
                    closeSilently(os);
                }

            }

            @Override
            protected String load(File file) throws IOException {
                System.out.println("load " + file.getAbsolutePath());
                InputStream is = new BufferedInputStream(new FileInputStream(file), bufferSize);
                try {
                    byte[] buffer = new byte[bufferSize];
                    int size = is.read(buffer);
                    return new String(buffer, 0, size);
                } finally {
                    closeSilently(is);
                }
            }
        };

        assertEquals(cache.limit(), 20);
        assertEquals(cache.capacity(), 0);

        assertNull(cache.get("1"));
        assertTrue(cache.put("1", "value01"));
        assertNull(cache.get("2"));
        assertTrue(cache.put("2", "value02"));
        assertNull(cache.get("3"));
        assertFalse(cache.put("3", "value-03"));
        assertNull(cache.get("4"));
        assertFalse(cache.put("4", "value-04"));
        assertNull(cache.get("5"));
        assertFalse(cache.put("5", "value-05"));

        assertNotNull(cache.get("1"));
        assertNotNull(cache.get("2"));
        assertNull(cache.get("3"));
        assertFalse(cache.put("3", "value-03"));
        assertNull(cache.get("4"));
        assertFalse(cache.put("4", "value-04"));
        assertNull(cache.get("5"));
        assertFalse(cache.put("5", "value-05"));

        cache.clear();
        assertEquals(cache.capacity(), 0);
        assertNull(cache.get("1"));
        assertNull(cache.get("2"));
    }
}
