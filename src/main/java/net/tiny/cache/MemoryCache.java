package net.tiny.cache;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A memory cache implementation which uses a LRU policy.
 * A cache that holds strong references to a limited number of objects.
 * Each time a object is accessed, it is moved to the head of a queue.
 * When a object is added to a full cache, the object at the end of that queue is evicted and may
 * become eligible for garbage collection.<br />
 * <br />
 * <b>NOTE:</b> This cache uses only strong references for stored object.
 *
 */
public abstract class MemoryCache<K, V> implements Cache<K, V> {

	public static final long MAX_AGE_SECOND = 30L; // Default 30 Seconds
	public static final int MAX_NORMAL_CACHE_SIZE = 16;
	public static final int MAX_NORMAL_CACHE_SIZE_IN_MB = MAX_NORMAL_CACHE_SIZE * 1024 * 1024; // Default 16M


	/**
	 * Contains strong references to stored objects (keys) and last object usage date (in milliseconds).
	 * If hard cache size will exceed limit then object with the least frequently usage is deleted (but it continue exist at
	 * {@link #softMap} and can be collected by GC at any time)
	 */
	private final Map<K, Reference<V>> softMap = Collections.synchronizedMap(new HashMap<K, Reference<V>>());
	private final Map<K, Policy<K>> policies = Collections.synchronizedMap(new HashMap<K, Policy<K>>());
	private final AtomicLong cacheSize;
	private int sizeLimit;
	private long maxAge;

	public MemoryCache() {
		this(MAX_NORMAL_CACHE_SIZE_IN_MB, MAX_NORMAL_CACHE_SIZE_IN_MB, MAX_AGE_SECOND);
	}

	/**
	 * @param maxAge Max object age <b>(in seconds)</b>.
	 */
	public MemoryCache(long maxAge) {
		this(MAX_NORMAL_CACHE_SIZE_IN_MB, MAX_NORMAL_CACHE_SIZE_IN_MB, maxAge);
	}

	/**
	 * @param size The limit size for cache
	 */
	public MemoryCache(int size) {
		this(size, MAX_NORMAL_CACHE_SIZE_IN_MB, MAX_AGE_SECOND);
	}

	/**
	 * @param size The limit size for cache
	 * @param maxAge Max object age <b>(in seconds)</b>. If object age will exceed this value then it'll be removed from
	 *               cache on next treatment (and therefore be reloaded).
	 */
	public MemoryCache(int size, long maxAge) {
		this(size, MAX_NORMAL_CACHE_SIZE_IN_MB, maxAge);
	}

	/**
	 * @param size The limit size for cache
	 * @param max Maximum size for cache (in bytes)
	 * @param maxAge Max object age <b>(in seconds)</b>. If object age will exceed this value then it'll be removed from
	 *               cache on next treatment (and therefore be reloaded).
	 */
	protected MemoryCache(int size, int max, long maxAge) {
		if(size < 1 && size > max) {
			throw new IllegalArgumentException("Illegal memory cache size " + size);
		}
		if(maxAge < 1) {
			throw new IllegalArgumentException("Illegal cache object age " + maxAge);
		}
		this.sizeLimit = size;
		this.cacheSize = new AtomicLong();
		this.maxAge = maxAge * 1000; // to milliseconds
	}

	@Override
	public boolean put(K key, V value) {
		// Try to add value to hard cache
		int valueSize = getSize(value);
		if (valueSize < sizeLimit) {
			while (capacity() + valueSize > sizeLimit) {
				if(null == removeNext())
					break;
			}
			if(capacity() + valueSize < sizeLimit) {
				// Add value to soft cache
				softMap.put(key, createReference(value));
				cacheSize.addAndGet(valueSize);
				// Add a policy of the key
				//policies.put(key, new Policy(key, valueSize));
				policies.put(key, new Policy<K>(key, valueSize));
				return true;
			} else {
				return false;
			}
		}
		return false;
	}

	@Override
	public V get(K key) {
		V value = null;
		Reference<V> reference = softMap.get(key);
		if (reference != null) {
			value = reference.get();
			if(null != value) {
				if(isOverAge(key)) {
					softMap.remove(key);
					// Remove the policy of key
					Policy<K> policy = policies.remove(key);
					if(null != policy)
						cacheSize.addAndGet(-policy.size);
				} else {
					// Count up
					hit(key);
				}
			} else {
				softMap.remove(key);
				// Remove the policy of key
				Policy<K> policy = policies.remove(key);
				if(null != policy)
					cacheSize.addAndGet(-policy.size);
			}
		}
		return value;
	}



	@Override
	public V remove(K key) {
		V value = null;
		Reference<V> reference = softMap.remove(key);
		if (reference != null) {
			value = reference.get();
			// Remove the policy of key
			Policy<K> policy = policies.remove(key);
			if(null != policy)
				cacheSize.addAndGet(-policy.size);
		}
		return value;
	}

	@Override
	public void clear() {
		softMap.clear();
		cacheSize.set(0);
		policies.clear();
	}

	@Override
	public int limit() {
		return sizeLimit;
	}

	@Override
	public int capacity() {
		return (int)cacheSize.get();
	}

	protected V removeNext() {
		Policy<K> minUsage = null;
		K leastUsedKey = null;
		Set<Entry<K, Policy<K>>> entries = policies.entrySet();
		synchronized (policies) {
			for (Entry<K, Policy<K>> entry : entries) {
				if (leastUsedKey == null) {
					leastUsedKey = entry.getKey();
					minUsage = entry.getValue();
				} else {
					Policy<K> lastUsage = entry.getValue();
					if (lastUsage.counter.get() < minUsage.counter.get()) {
						minUsage = lastUsage;
						leastUsedKey = entry.getKey();
					}
				}
			}
		}
		if(null == leastUsedKey)
			return null;
		return remove(leastUsedKey);
	}

	protected final void hit(K key) {
		Policy<K> policy = policies.get(key);
		if(null == policy) return;
		//Count up
		policy.counter.incrementAndGet();
		// Update age for key
		policy.age = System.currentTimeMillis();
	}

	protected final boolean isOverAge(K key) {
		Policy<K> policy = policies.get(key);
		if(null == policy) return false;
		return (System.currentTimeMillis() - policy.age > maxAge);
	}

	/** Creates {@linkplain Reference not strong} reference of value */
	protected Reference<V> createReference(V value) {
		return new WeakReference<V>(value);
	}

	@Override
	protected void finalize() {
		clear();
	}

	@Override
	public String toString() {
		return String.format(Locale.getDefault(), "Cache(%d): %d(%.2f%%) Age:%ds",
				softMap.size(), limit(), ((float)capacity()/(float)limit()), maxAge/1000L);
	}

	protected abstract int getSize(V value);
}
