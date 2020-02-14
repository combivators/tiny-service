package net.tiny.ws;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import net.tiny.config.Reflections;

/**
 * @see https://github.com/calebrob6/json-server
 * @see https://github.com/littleredhat1997/HTTPServer
 * @see https://github.com/hoddmimes/MicroServiceTemplate
 * @see https://github.com/kirgent/httpServerJson
 * @see https://github.com/ivan-lorenz/httpserver
 */
public class EmbeddedServer implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(EmbeddedServer.class.getName());

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    private static final long DEFAULT_STOP_TIME = 100L; //Delay 100ms
    private static final int DEFAULT_BACKLOG = 1;
    private static final int MIN_PORT = 8000;
    private static final int MAX_PORT = 9999;
    private static final int RANDOM_MIN_PORT = 8080;
    private static final int RANDOM_MAX_PORT = 8180;

    private final Builder builder;
    private HttpServer httpServer;
    private Throwable lastError;
    //Inner executor，External executor priority
    private ExecutorService executor;
    private String mark = "HTTP";
    private CountDownLatch serverLock = new CountDownLatch(1);

    private EmbeddedServer(Builder builder) {
        this.builder = builder;
    }

    public void listen(int port, Consumer<Callback<EmbeddedServer>> consumer) {
        builder.port(port);
        listen(consumer);
    }

    public void listen(String host, int port, Consumer<Callback<EmbeddedServer>> consumer) {
        builder.bind(host);
        builder.port(port);
        listen(consumer);
    }
    public void listen(Consumer<Callback<EmbeddedServer>> consumer) {
        try {
            listen();
            if(null != consumer) {
                consumer.accept(Callback.succeed(this));
            }
        } catch (Throwable e) {
            lastError = e;
            if(null != consumer) {
                consumer.accept(Callback.failed(e));
            }
        }
    }

    protected void listen() throws IOException {
        if (isStarted()) {
            return;
        }

        InetSocketAddress address;
        if("localhost".equalsIgnoreCase(builder.bind)) {
            address = new InetSocketAddress(builder.port) ;
        } else {
            address = new InetSocketAddress(builder.bind, builder.port) ;
        }

        String url;
        if (builder.ssl != null) {
            HttpsServer httpsServer = HttpsServer.create(address, builder.backlog);
            httpsServer.setHttpsConfigurator(builder.ssl.httpsConfigurator());
            httpServer = httpsServer;
            url = String.format("https://%s:%d", builder.bind, builder.port);
            mark = "HTTPS";
        } else {
            httpServer = HttpServer.create(address, builder.backlog);
            url = String.format("http://%s:%d", builder.bind, builder.port);
            mark = "HTTP";
        }

        if(null != builder.executor) {
            httpServer.setExecutor(builder.executor);
        } else {
            //创建访问进程池
            executor = Executors.newCachedThreadPool();
            httpServer.setExecutor(executor);
        }

        for (HttpHandler handler : builder.handlers) {
            try {
                if (handler instanceof WebServiceHandler) {
                    handle((WebServiceHandler)handler);
                } else {
                    handle(handler);
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, String.format("[%s:%d] Bind handler '%s' error : %s",
                        mark, builder.port, handler.toString(), e.getMessage()), e);
            }
        }

        httpServer.start();

        LOGGER.info(String.format("[%s:%d] Embedded server listen on %s", mark, builder.port, url));
    }

    @SuppressWarnings("unchecked")
    private void handle(HttpHandler handler) {
        final String contextPath = String.valueOf(Reflections.getFieldValue(handler, "path"));
        final HttpContext serverContext = httpServer.createContext(contextPath);
        serverContext.setHandler(handler);
        LOGGER.info(String.format("[%s:%d] bind a handler on '%s'", mark, builder.port, contextPath));

        try {
            //Set filter of handler
            List<Filter> hfs = (List<Filter>)Reflections.getFieldValue(handler, "filters");
            if (null != hfs) {
                List<Filter> filters = serverContext.getFilters();
                filters.addAll(hfs);
            }
        } catch (RuntimeException e) {}//Ignore

        try {
            //Set authenticator
            Authenticator auth = (Authenticator)Reflections.getFieldValue(handler, "auth");
            if (null != auth) {
                serverContext.setAuthenticator(auth);
            }
        } catch (RuntimeException e) {}//Ignore
    }

    protected void handle(WebServiceHandler handler) {
        final String contextPath = handler.path();
        final HttpContext serverContext = httpServer.createContext(contextPath);
        if (handler.isEndpoint()) {
            serverContext.getAttributes().put(ExecutorService.class.getName(), executor);
            //publish a endpoint
            handler.publish(serverContext);
            LOGGER.info(String.format("[%s:%d] publish a endpoint on '%s'", mark, builder.port, contextPath));
        } else {
            //serverContext.setHandler(handler.getBinding(HttpHandler.class));
            serverContext.setHandler(handler);
            LOGGER.info(String.format("[%s:%d] bind a handler on '%s'", mark, builder.port, contextPath));
        }

        //Set filter of handler
        if (handler.hasFilters()) {
            List<Filter> filters = serverContext.getFilters();
            filters.addAll(handler.getFilters());
        }

        //Set authenticator
        if (handler.isAuth()) {
            serverContext.setAuthenticator(handler.getAuth());
        }
    }

    protected void dispose() {
    }

    public int port() {
        return builder.port;
    }

    @Override
    public void close() {
        if (!isStarted()) {
            return;
        }
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    stopServer(builder.stopTimeout);
                } catch(Exception ex) {
                    lastError = ex;
                }
            }
        });
        task.start();
    }

    private void stopServer(long delay) throws Exception {
        try {
//            String[] names =
//                boundNames.keySet().toArray(new String[boundNames.size()]);
//             for(String name : names) {
//                 unbind(name);
//            }
            dispose();
            Thread.sleep(delay);
            shutdownExecutor(executor, delay);
            httpServer.stop(1);
        } finally {
            serverLock.countDown();
            httpServer = null;
            LOGGER.info(String.format("[%s:%d] Embedded server shutdowned", mark, builder.port));
        }
    }

    private void shutdownExecutor(ExecutorService pool, long timeout) {
        if (null == pool)
            return;
        pool.shutdown(); // Disable new tasks from being submitted
        try {
          // Wait a while for existing tasks to terminate
          if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
            pool.shutdownNow(); // Cancel currently executing tasks
            // Wait a while for tasks to respond to being cancelled
            if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                LOGGER.warning(String.format("[%s:%d] Pool did not terminate.", mark, builder.port));
            }
          }
        } catch (InterruptedException ie) {
          // (Re-)Cancel if current thread also interrupted
          pool.shutdownNow();
          // Preserve interrupt status
          Thread.currentThread().interrupt();
        }
        LOGGER.info(String.format("[%s:%d] Inner thread pool service closed.", mark, builder.port));
    }

    /////////////////////////////////////////////
    // Controllable methods
    public boolean start() {
        listen(callback -> {
            if(!callback.success()) {
                lastError = callback.cause();
            }
        });
        return lastError != null;
    }

    public void stop() {
        close();
    }

    public boolean isStarted() {
        return (null != httpServer);
    }

    public boolean hasError() {
        return (null != lastError);
    }

    public Throwable getLastError() {
        return lastError;
    }

    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        serverLock.await(timeout, unit);
    }

    public void awaitTermination() throws InterruptedException {
        serverLock.await();
    }

    public static boolean available(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }
        return false;
    }

    public static class SSL {
        String file;
        String password;
        boolean clientAuth = false;
        HttpsConfigurator https = null;

        public SSL file(String f) {
            file = f;
            return this;
        }
        public SSL password(String p) {
            password = p;
            return this;
        }
        public SSL auth(boolean enable) {
            clientAuth = enable;
            return this;
        }

        public HttpsConfigurator httpsConfigurator() {
            if(file == null || password == null || password.isEmpty()) {
                throw new IllegalArgumentException("KeyStore file or password is illegal.");
            }
            final char[] pass = password.toCharArray();
            try {
                // initialise the keystore
                KeyStore ks = KeyStore.getInstance ("JKS");
                FileInputStream fis = new FileInputStream(file);
                ks.load(fis, pass);

                // setup the key manager factory
                KeyManagerFactory kmf = KeyManagerFactory.getInstance ("SunX509");
                kmf.init(ks, pass);

                // setup the trust manager factory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);

                SSLContext sslContext = SSLContext.getInstance("TLS");
                // setup the HTTPS context and parameters
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                https = new SSLConfigurator(sslContext, clientAuth);
                return https;
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

    static class SSLConfigurator extends HttpsConfigurator {

        final boolean clientAuth;
        public SSLConfigurator(SSLContext context, boolean auth) {
            super(context);
            clientAuth = auth;
        }

        @Override
        public void configure(HttpsParameters params) {
            try {
                // initialize the SSL context
                SSLContext c = SSLContext.getDefault();
                SSLEngine engine = c.createSSLEngine();
                params.setNeedClientAuth(clientAuth);
                params.setCipherSuites(engine.getEnabledCipherSuites());
                params.setProtocols(engine.getEnabledProtocols());

                // get the default parameters
                SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                params.setSSLParameters(defaultSSLParameters);
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
        }
    }

    static class RandomPorts {
        final List<Integer> ports;
        final AtomicInteger counter;
        RandomPorts(int min, int max) {
            if(min == max) {
                throw new IllegalArgumentException("The value of min and max not be same.");
            }
            ports = new ArrayList<>();
            for (int i=Math.min(min, max); i<=Math.max(min, max); i++) {
                ports.add(i);
            }

            counter = new AtomicInteger();
        }

        public int next() {
            final int idx = counter.getAndIncrement() % ports.size();
            if (idx == 0) {
                Collections.shuffle(ports);
            }
            return ports.get(idx);
        }
    }

    public static class Builder {
        String bind = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        long stopTimeout = DEFAULT_STOP_TIME; //Delay 100ms
        int backlog = DEFAULT_BACKLOG;
        SSL ssl;
        ExecutorService executor;
        RandomPorts random;

        //List<WebServiceHandler> handlers = new ArrayList<>();
        List<HttpHandler> handlers = new ArrayList<>();
        private List<String> paths = new ArrayList<>();

        public Builder bind(String ip) {
            bind = ip;
            return this;
        }

        public Builder port(int p) {
            port = p;
            return this;
        }

        public Builder random() {
            if (null == random) {
                random = new RandomPorts(RANDOM_MIN_PORT, RANDOM_MAX_PORT);
            }
            port = random.next();
            while (!available(port)) {
                port = random.next();
            }
            return this;
        }

        public Builder backlog(int n) {
            backlog = n;
            return this;
        }

        public Builder executor(ExecutorService e) {
            executor = e;
            return this;
        }

        public Builder delay(long delay) {
            stopTimeout = delay;
            return this;
        }
        public Builder handlers(List<HttpHandler> list) {
            for (HttpHandler handler : list) {
                String path = "/";
                if (handler instanceof WebServiceHandler) {
                    path = ((WebServiceHandler)handler).path();
                } else {
                    path = String.valueOf(Reflections.getFieldValue(handler, "path"));
                }
                if (!paths.contains(path)) {
                    paths.add(path);
                    handlers.add(handler);
                }
            }
            return this;
        }

        public Builder handler(String path, HttpHandler handler) {
            if (!paths.contains(path)) {
                paths.add(path);
                if (handler instanceof WebServiceHandler) {
                   ((WebServiceHandler)handler).path(path);
                } else {
                    Reflections.setFieldValue(handler, "path", path);
                }
                handlers.add(handler);
            }
            return this;
        }

        public Builder ssl(String file, String password) {
            return ssl(file, password, false);
        }

        public Builder ssl(String file, String pass, boolean clientAuth) {
            ssl = new SSL()
                    .file(file)
                    .password(pass)
                    .auth(clientAuth);
            return this;
        }

        public EmbeddedServer build() {
            if (handlers.isEmpty()) {
                throw new IllegalArgumentException("Has not a http handler");
            }
            return new EmbeddedServer(this);
        }

        @Override
        public String toString() {
            return String.format("%s:%d", ssl != null ? "HTTPS" : "HTTP", port);
        }
    }
}