package net.tiny.ws.cache;

public interface Calculator<K, V> {
    V get(K key);
}
