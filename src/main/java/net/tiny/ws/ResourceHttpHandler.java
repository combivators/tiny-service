package net.tiny.ws;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.cache.ContentsCache;

/**
 * HTTP (HyperText Transfer Protocol) Handler
 * @see http://www.tohoho-web.com/ex/http.htm
 */
public class ResourceHttpHandler extends BaseWebService {

    private final Date lastModified = new Date(System.currentTimeMillis());
    private Map<String, String> resources = null;
    private ContentsCache contentCache = null;
    private List<String> paths = new ArrayList<>();
    private int cacheSize = -1;
    private long maxAge = 86400L; //1 day
    private String serverName = DEFALUT_SERVER_NAME;
    private ClassLoader loader = ResourceHttpHandler.class.getClassLoader();
    private boolean internal = false;


    public WebServiceHandler setPaths(List<String> paths) {
        this.paths = paths;
        return this;
    }

    public WebServiceHandler setInternal(boolean flag) {
        this.internal = flag;
        return this;
    }

    @Override
    protected boolean doGetOnly() {
        return true;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        // Go GET method only
        final String uri = he.getRequestURI().getPath();
        if (internal) {
            final URL res = findResouce(uri);
            sendResource(he, res);
        } else {
            final File doc = findLocalFile(uri);
            sendLocalFile(he, doc);
        }
     }

    private void sendLocalFile(HttpExchange he, File doc) throws IOException {
        byte[] buffer;
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        int statCode = HttpURLConnection.HTTP_OK;
        if (doc == null || !doc.exists() || !doc.isFile()) {
            header.setContentType(MIME_TYPE.HTML);
            buffer = NOT_FOUND;
            statCode = HttpURLConnection.HTTP_NOT_FOUND;
        } else {
            try {
                if (request.isNotModified(doc)) {
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
                    buffer = getCacheableContents(doc.toURI());
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

    private void sendResource(HttpExchange he, URL res) throws IOException {
        byte[] buffer;
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        int statCode = HttpURLConnection.HTTP_OK;
        if (res == null) {
            header.setContentType(MIME_TYPE.HTML);
            buffer = NOT_FOUND;
            statCode = HttpURLConnection.HTTP_NOT_FOUND;
        } else {
            try {
                if (request.isNotModified(lastModified.getTime())) {
                    buffer = new byte[0];
                    header.set("Server", serverName);
                    header.set("Connection", "Keep-Alive");
                    statCode = HttpURLConnection.HTTP_NOT_MODIFIED;
                } else {
                    header.setContentType(res.toString());
                    header.set(HEADER_LAST_MODIFIED, HttpDateFormat.format(lastModified));
                    header.set("Server", serverName);
                    header.set("Connection", "Keep-Alive");
                    header.set("Keep-Alive", "timeout=10, max=1000");
                    header.set("Cache-Control", "max-age=" + maxAge); //"max-age=0" 86400:1 day

                    buffer = getCacheableContents(res.toURI());
                    statCode = HttpURLConnection.HTTP_OK;
                }
            } catch (IOException | URISyntaxException e) {
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

    private byte[] getCacheableContents(URI res) throws IOException {
        if (cacheSize > 0 && contentCache == null) {
            // Cache max files
            contentCache = new ContentsCache(cacheSize);
        }
        if (contentCache != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("%s : Load '%s' contents from cache.",
                    getClass().getSimpleName(), res.toString()));
            }
            return contentCache.get(res);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(String.format("%s : Read contents from '%s'.",
                    getClass().getSimpleName(), res.toString()));
            }
            return readAllBytes(res);
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

    File findLocalFile(String uri) {
        int pos = uri.indexOf("/", 1);
        final Map<String, String> mapping = getResources();
        String path = mapping.get("/");
        if (pos == -1 && path == null) {
            // Not found "/" mapping
            return null;
        }

        Path real = null;
        if(pos > 0) {
            String res = mapping.get(uri.substring(1, pos));
            if (res == null) {
                if(path == null) {
                    return null;
                }
                //Not found in mapping resources
                real = Paths.get(path + uri);
            } else {
                real = Paths.get(res + uri.substring(uri.indexOf("/", 1)));
            }
        } else {
            // the resource on home
            real = Paths.get(path + uri);
        }
        if (real != null &&
            Files.exists(real) &&
            !Files.isDirectory(real) &&
            !Files.isSymbolicLink(real)) {
            return real.toFile();
        }
        return null;
    }

    URL findResouce(String uri) {
        int pos = uri.indexOf("/", 1);
        final Map<String, String> mapping = getResources();
        String path = mapping.get("/");
        if (pos == -1 && path == null) {
            // Not found "/" mapping
            return null;
        }
        String real = null;
        if(pos > 0) {
            String res = mapping.get(uri.substring(1, pos));
            if (res == null) {
                if(path == null) {
                    return null;
                }
                //Not found in mapping resources
                real = path + uri;
            } else {
                real = res + uri.substring(uri.indexOf("/", 1));
            }
        } else {
            // the resource on home
            real = path + uri;
        }
        return real != null ? loader.getResource(real) : null;
    }

    Map<String, String> getResources() {
        if (resources == null) {
            resources = new HashMap<>();
            for (String res : paths) {
                String[] array = res.split(":");
                if (array.length > 1) {
                    resources.put(array[0], array[1]);
                }
            }
        }
        return resources;
    }

    void setResources(Map<String, String> resources) {
        this.resources = resources;
    }
}
