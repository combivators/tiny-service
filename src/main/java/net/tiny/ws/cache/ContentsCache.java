package net.tiny.ws.cache;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class ContentsCache {

    private final BarakbCache<URI, byte[]> cache;

    /**
     * Create cache for the last capacity number used file.
     *
     * @param capacity
     */
    public ContentsCache(int capacity) {
        this.cache = new BarakbCache<>(key -> readContents(key), capacity);
        // Have an error from the file system that a file was deleted.
        this.cache.setRemoveableException(RuntimeException.class);
    }

    public void clear() {
        cache.clear();
    }

    public byte[] get(URI res) throws IOException {
        try {
            return cache.get(res);
        } catch (Throwable e) {
            Throwable cause = findErrorCause(e);
            if(cause instanceof IOException) {
                throw (IOException)cause;
            } else {
                throw new IOException(cause.getMessage(), cause);
            }
        }
    }

    @Override
    public String toString() {
        return cache.toString();
    }

    private Throwable findErrorCause(Throwable err) {
        if(err instanceof IOException)
            return err;
        Throwable cause = err.getCause();
        if (null != cause) {
            return findErrorCause(cause);
        } else {
            return err;
        }
    }

    private byte[] readContents(URI res) {
        try {
            return readAllBytes(res);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private byte[] readAllBytes(URI uri) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = uri.toURL().openStream();
        byte[] buffer = new byte[4096];
        int nread;
        while ((nread = in.read(buffer)) > 0) {
            baos.write(buffer, 0, nread);
        }
        baos.close();
        in.close();
        return baos.toByteArray();
    }
}
