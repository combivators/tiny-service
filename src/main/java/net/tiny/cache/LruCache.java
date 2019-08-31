package net.tiny.cache;

import java.util.LinkedHashMap;
import java.util.function.Function;

/**
 * https://teratail.com/questions/94242
 * https://github.com/barakb/Cache
 *
 * @param <K>
 * @param <V>
 */
public class LruCache<K, V> {

	final LinkedHashMap<K, V> map;
	Function<? super K, ? extends V> initializer;

	public LruCache(int capacity) {
		this.map = new LinkedHashMap<K, V>(capacity, 1.0F, true);
	}

	public V get(K key) {
		return map.computeIfAbsent(key, initializer);
	}
}
