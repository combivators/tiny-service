package net.tiny.ws;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.cache.CacheFunction;

/**
 * HTTP (HyperText Transfer Protocol) VirtualHost Handler
 */
public class VirtualHostHandler extends BaseWebService {

    final static String RELATIVE_PATH_REGEX = "[/]*[.][.]/.*";
    private List<VirtualHost> hosts = new ArrayList<>();
    private Map<String, String> mapping = null;
    private CacheFunction cache = null;
    private int cacheSize = -1;
    private long maxAge = 86400L; //1 day
    private String serverName = DEFALUT_SERVER_NAME;
    private boolean verbose = true;

    @Override
    protected boolean doGetOnly() {
        return true;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        // Go GET method only
        final String uri = he.getRequestURI().getPath();
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final String host = request.getHeader("Host");
        final File doc = findLocalFile(host, uri);
        final boolean unmodify = doc != null ? request.isNotModified(doc) : false;
        sendLocalFile(he, doc, unmodify);
     }

    private void sendLocalFile(HttpExchange he, File doc, boolean unmodify) throws IOException {
        byte[] buffer;
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        int statCode = HttpURLConnection.HTTP_OK;
        if (doc == null || !doc.exists() || !doc.isFile()) {
            header.setContentType(MIME_TYPE.HTML);
            buffer = NOT_FOUND;
            statCode = HttpURLConnection.HTTP_NOT_FOUND;
        } else {
            try {
                if (unmodify) {
                    buffer = new byte[0];
                    header.set("Server", serverName);
                    header.set("Connection", "Keep-Alive");
                    statCode = HttpURLConnection.HTTP_NOT_MODIFIED;
                } else {
                    header.setContentType(doc.getAbsolutePath());
                    header.setLastModified(doc);
                    header.set("Server", serverName);
                    header.set("Connection", "Keep-Alive");
                    header.set("Keep-Alive", "timeout=10, max=1000");
                    header.set("Cache-Control", "max-age=" + maxAge); //"max-age=0" 86400:1 day
                    buffer = getCacheableContents(doc.toURI().toURL());
                    statCode = HttpURLConnection.HTTP_OK;
                }
            } catch (IOException e) {
                header.setContentType(MIME_TYPE.HTML);
                buffer = SERVER_ERROR;
                statCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
            }
        }
        if (buffer.length > 0) {
            header.setContentLength(buffer.length);
            he.sendResponseHeaders(statCode, buffer.length);
            he.getResponseBody().write(buffer);
        } else {
            he.sendResponseHeaders(statCode, -1);
        }
    }

    private byte[] getCacheableContents(URL url) throws IOException {
        if (cache == null) {
            if (cacheSize > 0) {
                // Cache max files
                cache = new CacheFunction(cacheSize);
            } else {
                cache = new CacheFunction();
            }
        }
        return cache.apply(url);
    }

    String findVirtualHost(String virtual) {
        String path = mapping.get(virtual);
        if (path == null) {
            // Find host name without port
            path = mapping.get(virtual.split(":")[0]);
        }
        return path;
    }

    File findLocalFile(String virtual, String uri) {
        String path = findVirtualHost(virtual);
        if (path == null) {
            // Not found "/" mapping
            LOGGER.warning(String.format("[WEB] Not found '/' in virtual hosts mapping. See 'handler.virtual.hosts: {domain:%s, home:%s}' property is right or not.",
                    virtual, uri));
            return null;
        }

        if (Pattern.matches(RELATIVE_PATH_REGEX, uri)) {
            // Not found "/" mapping
            LOGGER.warning(String.format("[WEB] Not support relative path '%s' using virtual '%s'.", uri, virtual));
            return null;
        }
        // the resource on home
        final Path real = Paths.get(path + uri);
        if (verbose) {
           LOGGER.info(String.format("[WEB] Resource on home : '%s' for virtual '%s'", real, virtual));
        }
        if (real != null &&
            Files.exists(real) &&
            !Files.isDirectory(real) &&
            !Files.isSymbolicLink(real)) {
            return real.toFile();
        }
        return null;
    }

    public void setHosts(List<VirtualHost> virtuals) {
        this.hosts = virtuals;
        this.mapping = new HashMap<>();
        for (VirtualHost virtual : hosts) {
            this.mapping.put(virtual.domain(), virtual.home());
        }
    }
}
