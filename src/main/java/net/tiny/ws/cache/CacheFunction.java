package net.tiny.ws.cache;

import java.io.IOException;
import java.net.URL;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheFunction implements Function<URL,byte[]> {

    private final static Logger LOGGER = Logger.getLogger(CacheFunction.class.getName());

    private ContentsCache cache;

    public CacheFunction() {
        this(null);
    }

    public CacheFunction(int size) {
        cache = new ContentsCache(size);
    }

    public CacheFunction(ContentsCache cc) {
        cache = cc;
    }

    @Override
    public byte[] apply(URL url) {
        try {
            if (cache != null) {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("%s : Load '%s' contents from cache.",
                        getClass().getSimpleName(), url.toString()));
                }
                return cache.get(url);
            } else {
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine(String.format("%s : Read contents from '%s'.",
                        getClass().getSimpleName(), url.toString()));
                }
                return ContentsCache.readAllBytes(url);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(String.format("Read contents from '%s' error.", url.toString()));
        }
    }
}
