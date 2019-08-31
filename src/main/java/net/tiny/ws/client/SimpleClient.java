package net.tiny.ws.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import net.tiny.config.JsonParser;
import net.tiny.ws.Callback;

public class SimpleClient {

    public static final String MIME_TYPE_JSON  = "application/json;charset=utf-8";
    public static final String USER_AGENT      = "SimpleClient/" + System.getProperty("java.version");
    public static final String BROWSER_AGENT   = "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)";
    public static final String ACCEPT_MEDIA_TYPE = "application/json, text/*, image/*, audio/*, */*";
    private static final int DEFAULT_BUFFER_SIZE = 524288;

    private final Builder builder;
    private int status = -1;
    private byte[] contents = new byte[0];
    private Map<String, List<String>> headers = new HashMap<>();
    private HttpURLConnection connection = null;

    private SimpleClient(Builder builder) {
        this.builder = builder;
    }

    public int getStatus() {
        if(status == -1) {
            throw new IllegalStateException("Not yet connect to server.");
        }
        return status;
    }

    public byte[] getContents() {
        return getContents(false);
    }

    public byte[] getContents(boolean clone) {
        if(contents.length > 0 && clone) {
            byte[] buffer = new byte[contents.length];
            System.arraycopy(contents, 0, buffer, 0, contents.length);
            return buffer;
        }
        return contents;
    }

    public List<String> getHeaders(String name) {
        if (headers.containsKey(name)) {
            return headers.get(name);
        }
        // Capitalization sensitive
        for (String key : headers.keySet()) {
            if (name.equalsIgnoreCase(key))
                return headers.get(key);
        }
        return null;
    }

    public List<HttpCookie> getCookies() {
        return builder.cookieManager.getCookieStore().getCookies();
    }

    public String getCookie(String name) {
        Optional<HttpCookie> cookie =
                getCookies()
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
        return cookie.isPresent() ? cookie.get().getValue() : null;
    }

    public String getHeader(String name) {
        List<String> values = getHeaders(name);
        if (null != values)
            return values.get(0);
        return null;
    }

    public void close() {
        if(connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    public RequestBuilder request() {
        return request(null);
    }

    public RequestBuilder request(URL url) {
        return new RequestBuilder(this, url);
    }

    public <T> T doGet(String url, Class<T> type) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url)).doGet();
        if(null == buffer)
            return null;
        if (String.class.equals(type)) {
            return type.cast(new String(buffer));
        }
        return JsonParser.unmarshal(new String(buffer), type);
    }

    public String doGet(String url) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url)).doGet();
        if(null == buffer)
            return null;
        return new String(buffer);
    }

    public byte[] doGet(URL url, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        return new RequestBuilder(this, url).doGet(consumer);
    }

    protected void doGet(URL url, Map<String, List<String>> requestHeaders, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        for (String name : builder.headers.keySet()) {
            connection.setRequestProperty(name, builder.headers.get(name));
        }
        connection.setInstanceFollowRedirects(builder.redirects);

        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            for (String name : requestHeaders.keySet()) {
                List<String> values = requestHeaders.get(name);
                for (String value : values) {
                    connection.setRequestProperty(name, value);
                }
            }
        }

        headers.clear();
        status = connection.getResponseCode();
        final String msg = connection.getResponseMessage();
        headers.putAll(connection.getHeaderFields());
        boolean ok = (status >= 200 && status < 400);
        setContents(connection);

        if (!builder.keepAlive) {
            connection.disconnect();
        }
        if (null != consumer) {
            if (ok) {
                consumer.accept(Callback.succeed(this));
            } else {
                consumer.accept(Callback.failed(String.format("%d %s", status, msg)));
            }
        }
    }

    public String doPost(String url, String entity) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url))
                .doPost(entity.getBytes());
        if(null == buffer)
            return null;
        return new String(buffer);
    }

    public <T> T doPost(String url, Object entity, Class<T> type) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url)).doPost(JsonParser.marshal(entity).getBytes());
        if(null == buffer)
            return null;
        if (String.class.equals(type)) {
            return type.cast(new String(buffer));
        }
        return JsonParser.unmarshal(new String(buffer), type);
    }

    public byte[] doPost(URL url, byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        return new RequestBuilder(this, url).doPost(data, consumer);
    }

    protected void doPost(URL url, Map<String, List<String>> requestHeaders, byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        doSend("POST", url, requestHeaders, data, consumer);
    }

    public <T> T doPut(String url, Object entity, Class<T> type) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url)).doPut(JsonParser.marshal(entity).getBytes());
        if(null == buffer)
            return null;
        if (String.class.equals(type)) {
            return type.cast(new String(buffer));
        }
        return JsonParser.unmarshal(new String(buffer), type);
    }

    public String doPut(String url, String entity) throws IOException {
        byte[] buffer = new RequestBuilder(this, new URL(url)).doPut(entity.getBytes());
        if(null == buffer)
            return null;
        return new String(buffer);
    }

    public byte[] doPut(URL url, byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        return new RequestBuilder(this, url).doPut(data, consumer);
    }

    protected void doPut(URL url, Map<String, List<String>> requestHeaders, byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        doSend("PUT", url, requestHeaders, data, consumer);
    }

    void doSend(String method, URL url, Map<String, List<String>> requestHeaders, byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        for (String name : builder.headers.keySet()) {
            connection.setRequestProperty(name, builder.headers.get(name));
        }
        connection.setInstanceFollowRedirects(builder.redirects);
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            for (String name : requestHeaders.keySet()) {
                List<String> values = requestHeaders.get(name);
                for (String value : values) {
                    connection.setRequestProperty(name, value);
                }
            }
        }

        // POST data
        connection.setDoOutput(true);
        BufferedOutputStream bos = new BufferedOutputStream(connection.getOutputStream());
        bos.write(data);
        bos.flush();
        headers.clear();

        connection.connect();
        status = connection.getResponseCode();
        final String msg = connection.getResponseMessage();
        headers.putAll(connection.getHeaderFields());
        boolean ok = (status >=200 && status <=303);
        setContents(connection);

        if (!builder.keepAlive) {
            bos.close();
            connection.disconnect();
        }

        if (null != consumer) {
            if (ok) {
                consumer.accept(Callback.succeed(this));
            } else {
                consumer.accept(Callback.failed(String.format("%d %s", status, msg)));
            }
        }
    }

    private void setContents(HttpURLConnection conn) throws IOException {
        int size = conn.getContentLength();
        boolean has = false;
        if(size > 0) {
            has = true;
        }
        if (!has) {
            has = (null != conn.getContentType());
        }
        if (!has) {
            contents = new byte[0];
            return;
        }
        BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
        if (size < 0) {
            size = DEFAULT_BUFFER_SIZE;
        }
        contents = getContent(size, bis);
        bis.close();
    }

    private byte[] getContent(int contentLength, InputStream in) throws IOException {
        ByteArrayOutputStream contentBuffer = new ByteArrayOutputStream();
        byte readBuf[] = new byte[contentLength];
        int readLen = 0;
        while((readLen = in.read(readBuf)) > 0 ) {
            contentBuffer.write(readBuf, 0, readLen);
            contentBuffer.flush();
        }
        return contentBuffer.toByteArray();
    }

    public static class RequestBuilder {
        SimpleClient  client;
        boolean ssl = false;
        String protocol = "http";
        String host = "localhost";
        String path = "/";
        String query = null;
        int port = 80;
        Consumer<Callback<SimpleClient>> consumer;
        Map<String, List<String>> headers = new HashMap<>();

        public RequestBuilder(SimpleClient c, URL url) {
            client = c;
            if(url != null) {
                protocol = url.getProtocol();
                ssl = protocol.endsWith("s");
                host = url.getHost();
                port = url.getPort();
                path = url.getPath();
                query = url.getQuery();
            }
        }

        public RequestBuilder host(String h) {
            host = h;
            return this;
        }
        public RequestBuilder port(int p) {
            port = p;
            return this;
        }
        public RequestBuilder path(String p) {
            path = p;
            return this;
        }
        public RequestBuilder query(String q) {
            query = q;
            return this;
        }
        public RequestBuilder type(String t) {
            header("Content-type", t);
            return this;
        }
        public RequestBuilder accept(String t) {
            header("Accept", t);
            return this;
        }

        public RequestBuilder header(String name, String value) {
            List<String> values = headers.get(name);
            if(null == values) {
                values = new ArrayList<>();
                values.add(value);
                headers.put(name, values);
            } else if (!values.contains(value)) {
                values.add(value);
            }
            return this;
        }

        public byte[] doGet() throws IOException {
            return doGet(null);
        }

        public byte[] doGet(Consumer<Callback<SimpleClient>> consumer) throws IOException {
            String url;
            if (query == null) {
                url = String.format("%s://%s:%d%s", protocol, host, port, path);
            } else {
                url = String.format("%s://%s:%d%s?%s", protocol, host, port, path, query);
            }
            client.doGet(new URL(url), headers, consumer);
            return client.getContents(true);
        }


        public byte[] doPost(byte[] data) throws IOException {
            return doPost(data, null);
        }

        public byte[] doPost(byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
            return doSend("POST", data, consumer);
        }

        public byte[] doPut(byte[] data) throws IOException {
            return doPut(data, null);
        }

        public byte[] doPut(byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
            return doSend("PUT", data, consumer);
        }

        protected byte[] doSend(String method, byte[] data, Consumer<Callback<SimpleClient>> consumer) throws IOException {
            String url;
            if (query == null) {
                url = String.format("%s://%s:%d%s", protocol, host, port, path);
            } else {
                url = String.format("%s://%s:%d%s?%s", protocol, host, port, path, query);
            }
            client.doSend(method, new URL(url), headers, data, consumer);
            return client.getContents(true);
        }
    }

    public static class Builder {
        Map<String, String> headers = new HashMap<>();
        boolean redirects = false;
        boolean keepAlive = false;
        CookieManager cookieManager = new CookieManager();

        public Builder() {
            header("User-Agent", USER_AGENT);
            header("Accept", ACCEPT_MEDIA_TYPE);
            cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
            CookieHandler.setDefault(cookieManager);
        }

        public Builder redirects(boolean enable) {
            redirects = enable;
            return this;
        }


        public Builder userAgent(String ua) {
            header("User-Agent", ua);
            return this;
        }

        public Builder keepAlive(boolean enable) {
            if (enable) {
                header("Connection", "Keep-Alive");
                keepAlive = true;
            }
            return this;
        }

        public Builder accept(String mediaType) {
            header("Accept", mediaType);
            return this;
        }

        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        /**
         * Setting HTTP BASIC Authorization
         * @param name The name of authorization user
         *
         * @param pass
         *            The password of authorization user
         */
        public Builder credentials(String name, String pass) {
            // Basic Authorization should be Base64 encoded
            final String auth = name + ":" + pass;
            final String basicAuth = "Basic " + new String(Base64.getEncoder().encode(auth.getBytes()));
            return header("Authorization", basicAuth);
        }

        public SimpleClient build() {
            return new SimpleClient(this);
        }
    }
}
