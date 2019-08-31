package net.tiny.ws;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSession implements Serializable {

	private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>(8, 0.9f, 1);
    private final String id;
    private final long expired;
    private long timestamp;

    public ServerSession(String id, long expired) {
    	this(id, System.currentTimeMillis(), expired);
    }

    public ServerSession(String id, long timestamp, long expired) {
        this.id = id;
        this.timestamp = timestamp;
        this.expired   = expired;
    }

    public String getId() {
        return id;
    }

    public boolean isValid() {
    	return isValid(System.currentTimeMillis());
    }

    protected boolean isValid(long clock) {
    	return clock - this.timestamp < expired;
    }

    public ServerSession keepAlive() {
    	return keepAlive(System.currentTimeMillis());
    }

    protected ServerSession keepAlive(long clock) {
    	timestamp = clock;
        return this;
    }

    public boolean hasAttribute(String name) {
    	return attributes.containsKey(name);
    }

    public Object getAttribute(String name) {
    	return attributes.get(name);
    }

    public <T> T getAttribute(Class<T> type) {
    	final String name = type.getName();
    	return hasAttribute(name) ? type.cast(attributes.get(type.getName())) : null;
    }

    public void setAttribute(String name, Object value) {
    	attributes.put(name, value);
    }

    public <T> void setAttribute(Class<T> type, T value) {
    	attributes.put(type.getName(), value);
    }

    public Enumeration<String> getAttributeNames() {
    	return attributes.keys();
    }

    public void clear() {
    	attributes.clear();
    }

    public long getLastAccessedTime() {
    	return timestamp;
    }

    public static String getSessionFromCookie(String cookie) {
        return cookie.split("=")[1];
    }
}
