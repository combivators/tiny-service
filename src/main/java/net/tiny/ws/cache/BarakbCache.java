package net.tiny.ws.cache;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @see https://github.com/barakb/Cache
 *
 * @param <K>
 * @param <V>
 */
public class BarakbCache<K, V> {

    public static class SoftValue<K, V> extends SoftReference<V> {
        final K key;

        public SoftValue(V ref, ReferenceQueue<V> q, K key) {
            super(ref, q);
            this.key = key;
        }
    }

    private final int capacity;
    private final Calculator<K, V> compute;
    private final Map<K, SoftValue<K, Future<V>>> map;
    private final ExecutorService executor;
    private final ReferenceQueue<Future<V>> referenceQueue = new ReferenceQueue<Future<V>>();
    private ExceptionStrategy<K> exceptionStrategy;

    public BarakbCache(Calculator<K, V> compute, int capacity) {
        this(compute, new DirectExecutorService(), capacity);
    }

    /**
     * @param compute  procedure to compute the value
     * @param executor Do not pass direct executor or else you will have deadlock !
     * @param size     the size of the cache.
     */

    public BarakbCache(Calculator<K, V> compute, ExecutorService executor, int capacity) {
        this.capacity = capacity;
        this.exceptionStrategy = ExceptionStrategies.alwaysRetain();
        this.compute = compute;
        this.map = new LinkedHashMap<K, SoftValue<K, Future<V>>>(capacity, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, SoftValue<K, Future<V>>> eldest) {
                boolean ret = capacity < size();
                // if true, Evict see eldest.getKey()
                return ret;
            }
        };
        this.executor = executor;
    }

    public int size() {
        return map.size();
    }

    public synchronized SoftValue<K, Future<V>> remove(K key) {
        processQueue();
        return map.remove(key);
    }

    public synchronized void clear() {
        processQueue();
        map.clear();
    }

    public V get(final K key) throws Throwable {
        try {
            return getTask(key).get();
        } catch (ExecutionException e) {
            if (exceptionStrategy.removeEntry(key, e.getCause())) {
                // Removing entry from cache for the key, because of some exception.
                remove(key);
            }
            throw e.getCause();
        }
    }

    private synchronized Future<V> getTask(final K key) {
        processQueue();
        Future<V> ret;
        SoftReference<Future<V>> sr = map.get(key);
        if (sr != null) {
            ret = sr.get();
            if (ret != null) {
                return ret;
            }
        }
        ret = executor.submit(() -> compute.get(key));
        SoftValue<K, Future<V>> value = new SoftValue<>(ret, referenceQueue, key);
        map.put(key, value);
        return ret;
    }

    @SuppressWarnings("unchecked")
    private void processQueue() {
        while (true) {
            Reference<? extends Future<V>> o = referenceQueue.poll();
            if (null == o) {
                return;
            }
            SoftValue<K, Future<V>> k = (SoftValue<K, Future<V>>) o;
            K key = k.key;
            map.remove(key);
        }
    }

    public void setRemoveableException(final Class<? extends Throwable> cls) {
        this.exceptionStrategy = ExceptionStrategies.<K>removeOn(cls);
    }

    public void setExceptionStrategy(ExceptionStrategy<K> exceptionStrategy) {
        this.exceptionStrategy = exceptionStrategy;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "Cache(%d/%d)", map.size(), capacity);
    }

    static interface ExceptionStrategy<K> {
        /**
         *
         * @param key the key of the value that throws an exception
         * @param throwable the exception that was thrown
         * @return true iff this &lt;key, throwable&gt; pair should not be cached.
         */
        <T extends Throwable> boolean removeEntry(K key, T throwable);
    }

    static class ExceptionStrategies {
        public static <K> ExceptionStrategy<K> alwaysRetain() {
            return new ExceptionStrategy<K>() {
                @Override
                public <T extends Throwable> boolean removeEntry(K key, T throwable) {
                    return false;
                }
            };
        }

        public static <K> ExceptionStrategy<K> alwaysRemove() {
            return new ExceptionStrategy<K>() {
                @Override
                public <T extends Throwable> boolean removeEntry(K key, T throwable) {
                    return true;
                }
            };
        }

        public static <K> ExceptionStrategy<K> removeOn(final Class<? extends Throwable> cls) {
            return new ExceptionStrategy<K>() {
                @Override
                public <T extends Throwable> boolean removeEntry(K key, T throwable) {
                    return cls.isAssignableFrom(throwable.getClass());
                }
            };
        }

        public static <K> ExceptionStrategy<K> not(final ExceptionStrategy<K> strategy) {
            return new ExceptionStrategy<K>() {
                @Override
                public <T extends Throwable> boolean removeEntry(K key, T throwable) {
                    return !strategy.removeEntry(key, throwable);
                }
            };
        }

        @SuppressWarnings({"unchecked"})
        public static <K> ExceptionStrategy<K> and(final ExceptionStrategy<K>... strategies) {
            return new ExceptionStrategy<K>() {
                @Override
                public <T extends Throwable> boolean removeEntry(K key, T throwable) {
                    for (ExceptionStrategy<K> strategy : strategies) {
                        if (!strategy.removeEntry(key, throwable)) {
                            return false;
                        }
                    }
                    return true;
                }
            };
        }

        @SuppressWarnings({"unchecked"})
        public static <K> ExceptionStrategy<K> or(final ExceptionStrategy<K>... strategies) {
            return new ExceptionStrategy<K>() {
                @Override
                public <T extends Throwable> boolean removeEntry(K key, T throwable) {
                    for (ExceptionStrategy<K> strategy : strategies) {
                        if (strategy.removeEntry(key, throwable)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
        }

    }

    static class DirectExecutorService implements ExecutorService {

        private volatile boolean stopped;

        @Override
        public void shutdown() {
            stopped = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            stopped = true;
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
           return stopped;
        }

        @Override
        public boolean isTerminated() {
            return stopped;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return new DirectFutureTask<T>(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return new DirectFutureTask<T>(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return new DirectFutureTask<>(task, null);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return Collections.emptyList();
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            throw new InterruptedException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            throw new InterruptedException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new InterruptedException();
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    static class DirectFutureTask<V> extends FutureTask<V> {
        public DirectFutureTask(Callable<V> vCallable) {
            super(vCallable);
        }

        public DirectFutureTask(Runnable runnable, V result) {
            super(runnable, result);
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            super.run();
            return super.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            throw new IllegalArgumentException("Not implemented");
        }
    }
}
