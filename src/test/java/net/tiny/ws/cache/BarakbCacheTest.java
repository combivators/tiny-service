package net.tiny.ws.cache;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class BarakbCacheTest {

    private static ExecutorService executorService;

    @BeforeAll
    public static void beforeClass() {
        executorService = Executors.newFixedThreadPool(10);
    }

//    @ParameterizedTest.Parameters
//    public static Collection<Object[]> data() {
//        Object[][] data = new Object[][]{{new DirectExecutorService()},
//                {Executors.newCachedThreadPool()}};
//        return Arrays.asList(data);
//    }


    //@Test(timeout = 5000)
    @Test
    public void testGet() throws Throwable {
        final AtomicBoolean fromCache = new AtomicBoolean();
        BarakbCache<String, String> cache = new BarakbCache<>(key -> {
            fromCache.getAndSet(true);
            return key;
        }, executorService, 1);
        assertEquals("foo", cache.get("foo"));
        assertTrue(fromCache.getAndSet(false));
        assertEquals("foo", cache.get("foo"));
        assertFalse(fromCache.getAndSet(false));
        assertEquals("bar", cache.get("bar"));
        assertTrue(fromCache.getAndSet(false));
        assertEquals("foo", cache.get("foo"));
        assertTrue(fromCache.getAndSet(false));
    }

    //@Test(timeout = 5000)
    @Test
    public void testGetWaiting() throws Throwable {
        final AtomicInteger nResults = new AtomicInteger(0);
        final AtomicInteger nComputes = new AtomicInteger(0);
        final AtomicBoolean first = new AtomicBoolean(true);
        final CyclicBarrier computeBarrier = new CyclicBarrier(2);
        final BarakbCache<String, String> cache = new BarakbCache<>(key -> {
            if (first.compareAndSet(true, false)) {
                try {
                    computeBarrier.await();
                    computeBarrier.await();
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            }
            nComputes.incrementAndGet();
            return key;
        }, executorService, 1);

        final CyclicBarrier threadsBarrier = new CyclicBarrier(11);
        for (int i = 0; i < 10; ++i) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        threadsBarrier.await();
                        String value = cache.get("foo");
                        nResults.incrementAndGet();
                        assertEquals("foo", value);
                        threadsBarrier.await();
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            }.start();
        }
        threadsBarrier.await();
        computeBarrier.await();
        assertEquals(0, nResults.get());
        computeBarrier.await();
        threadsBarrier.await();
        assertEquals(10, nResults.get());
    }

    //@Test(timeout = 5000)
    @Test
    public void testDefaultExceptionStrategy() throws Throwable {
        //default strategy is to cache all exceptions. (alwaysRetain)
        final AtomicReference<Object> result = new AtomicReference<>(new IOException("foo"));
        BarakbCache<String, Object> cache = new BarakbCache<>(key -> {
            Object r = result.get();
            if (r instanceof IOException) {
                IOException e = (IOException) r;
                throw new RuntimeException(e.getMessage(), e);
            } else {
                return r;
            }
        }, executorService, 1);
        try {
            cache.get("foo");
            fail("should have thrown IOException");
        } catch (RuntimeException ignored) {
            assertTrue(ignored.getCause() instanceof IOException);
        }
        result.set("foo");
        try {
            cache.get("foo");
            fail("should have thrown IOException");
        } catch (RuntimeException ignored) {
            assertTrue(ignored.getCause() instanceof IOException);
        }
    }

    //@Test(timeout = 5000)
    @Test
    public void testAlwaysRemoveExceptionStrategy() throws Throwable {

        final AtomicReference<Object> result = new AtomicReference<>(new IOException("foo"));
        BarakbCache<String, Object> cache = new BarakbCache<>(key -> {
            Object r = result.get();
            if (r instanceof IOException) {
                IOException e = (IOException) r;
                throw new RuntimeException(e.getMessage(), e);
            } else {
                return r;
            }
        }, executorService, 1);
        cache.setExceptionStrategy(BarakbCache.ExceptionStrategies.<String>alwaysRemove());
        try {
            cache.get("foo");
            fail("should have thrown IOException");
        } catch (RuntimeException ignore) {
            assertTrue(ignore.getCause() instanceof IOException);
        }
        result.set("foo");
        assertEquals("foo", cache.get("foo"));
    }

    //@Test(timeout = 5000)
    @Test
    public void testCustomExceptionStrategy() throws Throwable {
        try {
        final AtomicReference<Object> result = new AtomicReference<>(new UnknownHostException("foo"));
        BarakbCache<String, Object> cache = new BarakbCache<>(key -> {
            Object r = result.get();
            if (r instanceof RuntimeException) {
                return (RuntimeException)r;
            } else if (r instanceof Exception) {
                Exception e = (Exception) r;
                throw new RuntimeException(e.getMessage(), e);
            } else {
                return r;
            }
        }, executorService, 1);
        cache.setExceptionStrategy(BarakbCache.ExceptionStrategies.<String>removeOn(IOException.class));
        try {
            cache.get("foo");
            fail("should have thrown UnknownHostException");
        } catch (RuntimeException ignore) {
            assertTrue(ignore.getCause() instanceof UnknownHostException);
        }
        result.set("foo");
        assertEquals("foo", cache.get("foo"));
        //noinspection ThrowableInstanceNeverThrown
        result.set(new IllegalArgumentException("bar"));
        try {
            cache.get("bar");
            fail("should have thrown IllegalArgumentException");
        } catch (RuntimeException ignore) {
            assertTrue(ignore.getCause() instanceof IllegalArgumentException);
        }
        result.set("bar");
        try {
            cache.get("bar");
            fail("should have thrown IllegalArgumentException");
        } catch (RuntimeException ignore) {
            assertTrue(ignore.getCause() instanceof IllegalArgumentException);
        }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test(timeout = 5000)
    @Test
    public void testLockingPolicy() throws Throwable {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final BarakbCache<String, Object> cache = new BarakbCache<>(key -> {
            if ("foo".equals(key)) {
                try {
                    barrier.await();
                    barrier.await();
                } catch (Exception ignored) {
                }
            }
            return key;
        }, executorService, 3);
        new Thread() {
            @Override
            public void run() {
                try {
                    String value = (String) cache.get("foo");
                    assertEquals(value, "foo");
                } catch (Throwable throwable) {
                    //logger.error(throwable.toString(), throwable);
                    fail(throwable.toString());
                }
            }
        }.start();

        barrier.await();
        assertEquals(cache.get("goo"), "goo");
        barrier.await();
    }
}
