package net.tiny.cache;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A memory cache interface.
 *
 */
public interface Cache<K, V> {

	class Policy<K> {
		final K key;
		final AtomicInteger counter;
		final long size;
		long age;

		Policy(K k, long s) {
			key = k;
			size = s;
			counter = new AtomicInteger();
			age = System.currentTimeMillis();
		}
	}

	class UsingFreq<K> implements Comparator<Policy<K>> {
		@Override
		public int compare(Policy<K> p1, Policy<K> p2) {
			return p2.counter.get() - p1.counter.get();
		}
	}

	class LimitedAge<K> implements Comparator<Policy<K>> {
		@Override
		public int compare(Policy<K> p1, Policy<K> p2) {
			return (int)(p1.age - p2.age);
		}
	}

	/**
     * Gets an value for the specified {@code key} or return {@code null}.
     *
     * @param key key
     * @return the value or {@code null}.
     */
    V get(K key);

    /**
     * Puts an value in the cache for the specified {@code key}.
     *
     * @param key   key
     * @param value value
	 * @return <b>true</b> - if value was put into cache successfully,
	 *         <b>false</b> - if value was <b>not</b> put into cache
	 *
     */
    boolean put(K key, V value);

    /**
     * Removes the entry for {@code key} if it exists or return {@code null}.
     *
     * @return the previous value or @{code null}.
     */
    V remove(K key);

    /**
     * Clears all the entries in the cache.
     */
    void clear();

    /**
     * Returns the max memory size of the cache.
     *
     * @return max memory size.
     */
    int limit();

    /**
     * Returns the current memory size of the cache.
     *
     * @return current memory size.
     */
    int capacity();

}
