package net.tiny.ws;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
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

import com.sun.net.httpserver.HttpExchange;

import net.tiny.ws.cache.CacheFunction;

/**
 * HTTP (HyperText Transfer Protocol) Handler
 * @see http://www.tohoho-web.com/ex/http.htm
 */
public class ResourceHttpHandler extends BaseWebService {

    private final Date lastModified = new Date(System.currentTimeMillis());
    private Map<String, String> resources = null;
    private CacheFunction cache = null;
    private List<String> paths = new ArrayList<>();
    private int cacheSize = -1;
    private long maxAge = 86400L; //1 day
    private String serverName = DEFALUT_SERVER_NAME;
    private ClassLoader loader = ResourceHttpHandler.class.getClassLoader();
    private boolean internal = false;
    private boolean verbose = false;

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
            final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
            final boolean unmodify = request.isNotModified(doc);
            sendLocalFile(he, doc, unmodify);
        }
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

    private void sendResource(HttpExchange he, URL url) throws IOException {
        byte[] buffer;
        final RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        int statCode = HttpURLConnection.HTTP_OK;
        if (url == null) {
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
                    header.setContentType(url.toURI().toString());
                    header.set(HEADER_LAST_MODIFIED, HttpDateFormat.format(lastModified));
                    header.set("Server", serverName);
                    header.set("Connection", "Keep-Alive");
                    header.set("Keep-Alive", "timeout=10, max=1000");
                    header.set("Cache-Control", "max-age=" + maxAge); //"max-age=0" 86400:1 day

                    buffer = getCacheableContents(url);
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


    File findLocalFile(String uri) {
        int pos = uri.indexOf("/", 1);
        final Map<String, String> mapping = getResources();
        String path = mapping.get("/");
        if (pos == -1 && path == null) {
            // Not found "/" mapping
            LOGGER.warning("[WEB] Not found '/' in local resource mapping. See 'handler.resource.paths=/:{local path}' property is right or not.");
            return null;
        }

        Path real = null;
        if(pos > 0) {
            String res = mapping.get(uri.substring(1, pos));
            if (res == null) {
                if(path == null) {
                    if (verbose) {
                        LOGGER.warning(String.format("[WEB] Lookup:'%s', path is null.", real));
                    }
                    return null;
                }
                //Not found in mapping resources
                real = Paths.get(path + uri);
                if (verbose) {
                    LOGGER.info(String.format("[WEB] Try to find '%s' > '%s'", (path + uri), real));
                }
            } else {
                real = Paths.get(res + uri.substring(uri.indexOf("/", 1)));
                if (verbose) {
                    LOGGER.info(String.format("[WEB] Mapping resources : '%s'", real));
                }
            }
        } else {
            // the resource on home
            real = Paths.get(path + uri);
            if (verbose) {
                LOGGER.info(String.format("[WEB] Resource on home : '%s'", real));
            }
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
            if (verbose) {
                LOGGER.warning("[WEB] Not found \"/\" mapping");
            }
            return null;
        }
        String real = null;
        if(pos > 0) {
            real = uri.substring(1, pos);
            String res = mapping.get(real);
            if (res == null) {
                if(path == null) {
                    if (verbose) {
                        LOGGER.warning(String.format("[WEB] lookup:'%s', path is null.", real));
                    }
                    return null;
                }
                //Not found in mapping resources
                real = path + uri;
                if (verbose) {
                    LOGGER.info(String.format("[WEB] Not found in mapping resources. Local:'%s'", real));
                }
            } else {
                real = res + uri.substring(uri.indexOf("/", 1));
            }
        } else {
            // the resource on home
            real = path + uri;
            if (verbose) {
                LOGGER.info(String.format("[WEB] Found '%s' on home", real));
            }
        }
        if (real != null && real.endsWith("/")) {
            real = real.concat("index.html");
        }
        if (verbose) {
            LOGGER.info(String.format("[WEB] Load local '%s'", real));
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
                    if (verbose) {
                        LOGGER.info(String.format("[HTTP(s)] static mapping : '%s'='%s'", array[0], array[1]));
                    }
                }
            }
        }
        return resources;
    }

    void setResources(Map<String, String> resources) {
        this.resources = resources;
    }
}
