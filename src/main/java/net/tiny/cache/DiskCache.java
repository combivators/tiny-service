package net.tiny.cache;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A disk cache implementation which uses a LRU policy.
 * A cache that holds strong references to a limited number of objects.
 * Each time a object is accessed, it is moved to the head of a queue.
 * When a object is added to a full cache, the object at the end of that queue is evicted and may
 * become eligible for garbage collection.<br />
 * <br />
 * <b>NOTE:</b> This cache uses only strong references for stored object.
 *
 */
public abstract class DiskCache<K, V> implements Cache<K, V> {

	private static final long MAX_AGE_SECOND = 24L*60L*60L; // Default 24H
	private static final long MAX_NORMAL_CACHE_SIZE = 64;
	private static final long MAX_NORMAL_CACHE_SIZE_IN_MB = MAX_NORMAL_CACHE_SIZE * 1024 * 1024; // Default 64M

	private static final String HASH_ALGORITHM = "MD5";
	private static final int RADIX = 10 + 26; // 10 digits + 26 letters

	class FilenameGenerator {
		public String generate(String name) {
			byte[] md5 = getMD5(name.getBytes());
			BigInteger bi = new BigInteger(md5).abs();
			return bi.toString(RADIX);
		}

		private byte[] getMD5(byte[] data) {
			byte[] hash = null;
			try {
				MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
				digest.update(data);
				hash = digest.digest();
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			return hash;
		}
	}

	/** Stores object to cache file */
	private final Map<K, File> fileCache = Collections.synchronizedMap(new HashMap<K, File>());
	private final Map<K, Policy<K>> policies = Collections.synchronizedMap(new HashMap<K, Policy<K>>());
	private final FilenameGenerator filenameGenerator = new FilenameGenerator();
	private final AtomicLong cacheSize;
	private final File cacheDir;
	private long sizeLimit;
	private long maxAge;


	/**
	 * @param path The directory for cache
	 */
	public DiskCache(String path) {
		this(new File(path), MAX_NORMAL_CACHE_SIZE_IN_MB, MAX_NORMAL_CACHE_SIZE_IN_MB, MAX_AGE_SECOND);
	}

	/**
	 * @param path The directory for cache
	 * @param size The limit size for cache
	 * @param maxAge Max object age <b>(in seconds)</b>. If object age will exceed this value then it'll be removed from
	 *               cache on next treatment (and therefore be reloaded).
	 */
	public DiskCache(String path, long size, long maxAge) {
		this(new File(path), size, MAX_NORMAL_CACHE_SIZE_IN_MB, maxAge);
	}

	/**
	 * @param dir The directory for cache
	 * @param size The limit size for cache
	 * @param max Maximum size for cache (in bytes)
	 * @param maxAge Max object age <b>(in seconds)</b>. If object age will exceed this value then it'll be removed from
	 *               cache on next treatment (and therefore be reloaded).
	 */
	protected DiskCache(File dir, long size, long max, long maxAge) {
		if(size < 1 && size > max) {
			throw new IllegalArgumentException("Illegal disk cache size " + size);
		}
		if(maxAge < 1) {
			throw new IllegalArgumentException("Illegal cache object age " + maxAge);
		}
		if(dir == null || !dir.exists() || !dir.isDirectory()) {
			throw new IllegalArgumentException("Illegal cache path " + (dir == null ? "null" : dir.getAbsolutePath()));
		}
		this.cacheDir = dir;
		this.sizeLimit = size;
		this.cacheSize = new AtomicLong();
		this.maxAge = maxAge * 1000; // to milliseconds
	}

	@Override
	public boolean put(K key, V value) {
		// Try to add value to hard cache
		try {
			long valueSize = getSize(value);
			if (valueSize < sizeLimit) {
				while (capacity() + valueSize > sizeLimit) {
					if(null == removeNext())
						break;
				}
				if(capacity() + valueSize < sizeLimit) {
					File file = generateFile(key, value);
					valueSize = file.length();
					// Add value to file cache
					fileCache.put(key, file);
					cacheSize.addAndGet(valueSize);
					// Add a policy of the key
					policies.put(key, new Policy<K>(key, valueSize));
					return true;
				} else {
					return false;
				}
			}
			return false;
		} catch (IOException e) {
			throw new IllegalStateException("'" + key + "' : " + e.getMessage());
		}
	}

	@Override
	public V get(K key) {
		V value = null;
		File file = fileCache.get(key);
		if (file != null) {
			try {
				value = load(file);
				if(null != value) {
					if(isOverAge(key)) {
						fileCache.remove(key);
						file.delete();
						// Remove the policy of key
						Policy<K> policy = policies.remove(key);
						if(null != policy)
							cacheSize.addAndGet(-policy.size);
					} else {
						// Count up
						hit(key);
					}
				} else {
					fileCache.remove(key);
					file.delete();
					// Remove the policy of key
					Policy<K> policy = policies.remove(key);
					if(null != policy)
						cacheSize.addAndGet(-policy.size);
				}
			} catch (IOException e) {
				fileCache.remove(key);
				file.delete();
				// Remove the policy of key
				Policy<K> policy = policies.remove(key);
				if(null != policy)
					cacheSize.addAndGet(-policy.size);
				throw new IllegalStateException("'" + key + "' : " + e.getMessage());
			}
		}
		return value;
	}

	@Override
	public V remove(K key) {
		V value = null;
		File file = fileCache.remove(key);
		if (file != null) {
			try {
				value = load(file);
			} catch (IOException e) {
				throw new IllegalStateException("'" + key + "' : " + e.getMessage());
			} finally {
				// Remove the policy of key
				Policy<K> policy = policies.remove(key);
				if(null != policy)
					cacheSize.addAndGet(-policy.size);
				file.delete();
			}
		}
		return value;
	}

	@Override
	public void clear() {
		Collection<File> files = fileCache.values();
		synchronized (fileCache) {
			for (File file : files) {
				if(file.exists()) {
					file.delete();
				}
			}
		}
		fileCache.clear();
		cacheSize.set(0);
		policies.clear();
	}

	@Override
	public int limit() {
		return (int)sizeLimit;
	}

	@Override
	public int capacity() {
		return (int)cacheSize.get();
	}

	protected V removeNext() {
		K leastUsedKey = null;
		synchronized (policies) {
			// Sort policy by counter
			TreeSet<Policy<K>> sorter = new TreeSet<Policy<K>>(new UsingFreq<K>());
			sorter.addAll(policies.values());
			if(sorter.first().counter.get() > sorter.last().counter.get()) {
				leastUsedKey = sorter.last().key;
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

	protected File getCacheDir() {
		return cacheDir;
	}

	protected File generateFile(K key, V value) throws IOException {
		File file = new File(cacheDir, filenameGenerator.generate(key.toString()));
		if(!file.exists()) {
			save(file, value);
		}
		return file;
	}

	@Override
	public String toString() {
		return String.format(Locale.getDefault(), "Cache(%d): %d(%.2f%%) Age:%ds",
				fileCache.size(), limit(), ((float)capacity()/(float)limit()), maxAge/1000L);
	}

	public static void closeSilently(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception ignored) {
			}
		}
	}

	protected abstract int getSize(V value);
	protected abstract void save(File file, V value) throws IOException;
	protected abstract V load(File file) throws IOException;
}
