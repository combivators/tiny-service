package net.tiny.ws.cache;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Function;

/**
 * https://teratail.com/questions/94242
 * https://github.com/barakb/Cache
 *
 * @param <K>
 * @param <V>
 */
public class Cache<K, V> {

    static class NonCache<K, V> implements Calculator<K, V> {
        private Function<K, V> initializer;

        NonCache(Function<K, V> initializer) {
            this.initializer = initializer;
        }

        @Override
        public V get(K key) {
            return initializer.apply(key);
        }
    }

    static class NonLruCache<K, V> implements Calculator<K, V> {
        private int capacity;
        private Function<K, V> initializer;
        private Map<K, V> map = new LinkedHashMap<>(capacity, 1.0f, true);

        NonLruCache(int capacity, Function<K, V> initializer) {
            this.capacity = capacity;
            this.initializer = initializer;
        }

        @Override
        public V get(K key) {
            return map.computeIfAbsent(key, initializer);
        }
    }

    static class LruCache1<K, V> implements Calculator<K, V> {
        private int capacity;
        private Function<K, V> initializer;
        private Map<K, V> map = Collections.synchronizedMap(
            new LinkedHashMap<K,V>(capacity,1.0f,true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<K,V>eldest) {
                    return size()>capacity;
                }
            });

        LruCache1(int capacity, Function<K, V> initializer) {
            this.capacity = capacity;
            this.initializer = initializer;
        }

        @Override
        public V get(K key) {
            return map.computeIfAbsent(key, initializer);
        }
    }

    static class LruCache2<K, V> implements Calculator<K, V> {
        private int capacity;
        private StampedLock lock = new StampedLock();
        private Function<K, V> initializer;

        private Map<K, V> map = new LinkedHashMap<K, V>(capacity, 1.0f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
                return size() > capacity;
            }
        };

        LruCache2(int capacity, Function<K, V> initializer) {
            this.capacity = capacity;
            this.initializer = initializer;
        }

        @Override
        public V get(K key) {
            long stamp = lock.writeLock();
            try {
                return map.computeIfAbsent(key, initializer);
            } finally {
                lock.unlockWrite(stamp);
            }
        }
    }

    static class LruCache3<K, V> implements Calculator<K, V> {
        private int capacity;
        private ReentrantLock lock = new ReentrantLock();
        private Function<K, V> initializer;

        private Map<K, V> map = new LinkedHashMap<K, V>(capacity, 1.0f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
                return size() > capacity;
            }
        };

        LruCache3(int capacity, Function<K, V> initializer) {
            this.capacity = capacity;
            this.initializer = initializer;
        }

        @Override
        public V get(K key) {
            lock.lock();
            try {
                return map.computeIfAbsent(key, initializer);
            } finally {
                lock.unlock();
            }
        }
    }


    static class LruCache4<K, V> implements Calculator<K, V> {
        private BarakbCache<K, V> cache;

        LruCache4(int capacity, Function<K, V> initializer) {

            final Calculator<K, V> c = new Calculator<K, V>() {
                @Override
                public V get(K key) {
                    return initializer.apply(key);
                }
            };
            this.cache = new BarakbCache<>(c, capacity);
        }

        @Override
        public V get(K key) {
            try{
                return cache.get(key);
            } catch(Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

}
