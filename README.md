## Tiny Service: 一个基于Web系统精简的微服框架
## 设计目的
 - 内置HTTP服务器引擎:com.sun.net.httpserver.HttpsServer。
 - 提供极简的HTTP(S)的MicroService模式。
 - 使用Tiny boot包进行服务器配置。
 - 内置标准静态Web服务器以及控制接口(API)。
 - 内置虚拟Web服务器。
 - 支持文件系统快速缓存(Lur Cache)。
 - 提供精简的HTTP客户端Java类。
 - 提供微服之间处理异步消息流的框架。
 - 提供用于微服认证的JSON Web Token(JWT)生成器。

##Usage

###1. Simple Run
```java
java net.tiny.boot.Main --verbose --profile local
```


###2. Application configuration file with profile
 - Configuration file : application-{profile}.[yml, json, conf, properties]

```txt
logging:
  handler:
    output: none
    level: FINE
  level:
    all:      ALL
    jdk:      WARNING
    java:     WARNING
    javax:    WARNING
    com.sun:  WARNING
    sun.net:  WARNING
    sun.util: WARNING
    net.tiny: INFO
main:
  - ${launcher.http}
  - ${launcher.https}
  - ${launcher.one8081}
  - ${launcher.two8082}
daemon: true
executor: ${pool}
callback: ${service.context}
pool:
  class:   net.tiny.service.PausableThreadPoolExecutor
  size:    10
  max:     30
  timeout: 3
launcher:
  http:
    class: net.tiny.ws.Launcher
    builder:
      port: 8080
      backlog: 5
      stopTimeout: 1
      executor: ${pool}
      handlers:
        - ${handler.sys}
        - ${handler.home}
  https:
    class: net.tiny.ws.Launcher
    builder:
      port: 8443
      backlog: 5
      stopTimeout: 1
      executor: ${pool}
      handlers:
        - ${handler.health}
      ssl:
        file:       src/test/resources/ssl/server.jks
        password:   changeit
        clientAuth: false
  one8081:
    class: net.tiny.ws.Launcher
    builder:
      port: 8081
      backlog: 5
      stopTimeout: 1
      executor: ${pool}
      handlers: ${handler.virtual}
  two8082:
    class: net.tiny.ws.Launcher
    builder:
      port: 8082
      backlog: 5
      stopTimeout: 1
      executor: ${pool}
      handlers: ${handler.virtual}
handler:
  sys:
    class:   net.tiny.ws.ControllableHandler
    path:    /sys
    auth:    ${auth.simple}
    filters:
      - ${filter.logger}
      - ${filter.snap}
  health:
    class:   net.tiny.ws.VoidHttpHandler
    path:    /health
    filters: ${filter.logger}
  home:
    class:    net.tiny.ws.ResourceHttpHandler
    verbose:  true
    cacheSize: 100
    internal:  false
    path:      /home
    paths:     home:src/test/resources/home
    filters:
      - ${filter.logger}
      - ${filter.cors}
      - ${filter.snap}
  virtual:
    class:     net.tiny.ws.VirtualHostHandler
    verbose:   true
    cacheSize: 100
    path:      /
    filters:   ${filter.virtual}
    hosts:
      - ${host.virtual.one}
      - ${host.virtual.two}
      - ${host.virtual.three1}
      - ${host.virtual.three2}
host:
  virtual:
    one:
      domain: one.localdomain
      home:   src/test/resources/virtual/one
      log:    .access.log
    two:
      domain: two.localdomain
      home:   src/test/resources/virtual/two
      log:    .access.log
    three1:
      domain: three.localdomain:8081
      home:   src/test/resources/virtual/three-8081
      log:    stdout
    three2:
      domain: three.localdomain:8082
      home:   src/test/resources/virtual/three-8082
      log:    stderr
filter:
   logger:
     class: net.tiny.ws.AccessLogger
     out:   stdout
   virtual:
     class: net.tiny.ws.VirtualLogger
     hosts:
       - ${host.virtual.one}
       - ${host.virtual.two}
       - ${host.virtual.three1}
       - ${host.virtual.three2}
   cors:
     class: net.tiny.ws.CorsResponseFilter
   snap:
     class: net.tiny.ws.SnapFilter
auth:
  simple:
    class:    net.tiny.ws.auth.SimpleAuthenticator
    token:    DES:CAhQn4bV:HIOsSQIg
    encode:   true
    username: user
    password: Piz5wX49L4MS4SYsGwEMNw==
service:
  context:
    class: net.tiny.service.ServiceLocator
  monitor:
    class: net.tiny.service.ServiceContext$Monitor
content:
  cache:
    class: net.tiny.ws.cache.CacheFunction
    size: 10
vcap:
  alias: vcap.services.ups-tiny.credentials
```

###3. The first simple application none tiny packages dependency

```java
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class HelloHandler implements HttpHandler {

    protected String path;
    protected List<Filter> filters = new ArrayList<>(); //Option
    protected Authenticator auth = null;  //Option

    @Override
    public void handle(HttpExchange he) throws IOException {
        final String method = he.getRequestMethod();
        final String name = getURIParameters(he, 0);
        final String responseBody = String.format("{\"method\":\"%s\", \"msg\":\"Hello %s\"}", method, name);
        final byte[] rawResponseBody = responseBody.getBytes(StandardCharsets.UTF_8);
        he.getResponseHeaders().add("Content-type", "application/json; charset=utf-8");
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, rawResponseBody.length);
        he.getResponseBody().write(rawResponseBody);
    }

    private String getURIParameters(final HttpExchange he, final int index) {
        final String requestPath = he.getHttpContext().getPath();
        final String uri = he.getRequestURI().toString();
        // easy case no sub item relative path
        int i = uri.indexOf(requestPath);
        if (i >= 0) {
            String uriParameters = (uri.length() > requestPath.length()) ?
                    uri.substring( i + requestPath.length() + 1 ) : uri.substring( i + requestPath.length());
            // Request path : "foo/fie/bar" --> "foo","fie","bar"
            try {
                return uriParameters.split("/")[index];
            } catch (Exception e) {}
        }
        return "";

    }

}
```

###4. Sample MicroService java

```java
import net.tiny.ws.BaseWebService;
import net.tiny.ws.RequestHelper;
import net.tiny.ws.ResponseHeaderHelper;

public class SimpleJsonHandler extends BaseWebService {
    @Override
    protected boolean doGetOnly() {
        return true;
    }

    @Override
    protected void execute(HTTP_METHOD method, HttpExchange he) throws IOException {
        // Do GET method only
        RequestHelper request = HttpHandlerHelper.getRequestHelper(he);
        final Map<String, List<String>> requestParameters = request.getParameters();
        // do something with the request parameters
        final String responseBody = "['hello world!']";
        final ResponseHeaderHelper header = HttpHandlerHelper.getHeaderHelper(he);
        header.setContentType(MIME_TYPE.JSON);
        he.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseBody.length);
        he.getResponseBody().write( responseBody.getBytes());
    }
}
```

###5. Sample Endpoint java

```java
@WebService(name = "NS", targetNamespace = "http://ws.tiny.net/")
@SOAPBinding(style = SOAPBinding.Style.RPC)
public interface CalculatorService {
    @WebMethod
    int sum(int a, int b);
    @WebMethod
    int diff(int a, int b);
    @WebMethod
    int multiply(int a, int b);
    @WebMethod
    int divide(int a, int b);
}


@WebService(serviceName = "CalculatorService",
        portName = "CalculatorServicePort",
        endpointInterface = "mypackage.CalculatorService")
public class CalculatorServer extends AbstractWebService implements CalculatorService {
    @Override
    public int sum(int a, int b) {
        return a+b;
    }

    @Override
    public int diff(int a, int b) {
        return a-b;
    }

    @Override
    public int multiply(int a, int b) {
        return a*b;
    }

    @Override
    public int divide(int a, int b) {
        return a/b;
    }
}
```


###4. Sample HTTP client java
```java
import net.tiny.ws.client.SimpleClient;

SimpleClient client = new SimpleClient.Builder()
        .userAgent(BROWSER_AGENT)
        .keepAlive(true)
        .build();

client.doGet(new URL("http://localhost:8080/css/style.css"), callback -> {
    if(callback.success()) {
        // If status is HTTP_OK(200)
    } else {
        Throwable err = callback.cause();
        // If status <200 and >305
    }
});

Date lastModified = HttpDateFormat.parse(client.getHeader("Last-modified"));

client.request().port(port).path("/css/style.css")
    .header("If-Modified-Since", HttpDateFormat.format(lastModified))
    .doGet(callback -> {
        if(callback.success()) {
            // If status is HTTP_NOT_MODIFIED(304)
        } else {
            Throwable err = callback.cause();
            // If status <200 and >305
        }
    });

client.close();
```


###5. Message Bus Java Sample
```java
import net.tiny.message.Bus;

Task task = new Task();
Bus<String> bus = Bus.getInstance(String.class);

//register to a message
bus.register(task, "channel1", value -> task.exec(value));

//send a message
bus.publish("channel1", "Hello One");

//remove registered channel
bus.remove("channel1", task);
bus.clear("channel1");
Bus.destroy(String.class);
```

##微服认证JWT生成器
## 重要提示
**注意:** 生成 Token 需要以超级密钥为参数，所以应该仅在可信赖的服务器上生成 Token。另外，决不可把超级密钥存入应用程序中，也不要与客户端分享超级密钥。
## 生成JWT方法

`Options` 对象有下面几个方法：

* **expires(Date)** - 设置过期时间点，此时间过后 Token 失效。

* **notBefore(Date)** - 设置生效时间点，到达此时间后 Token 才可用。

* **admin(boolean)** - 若为 `true` ，规则表达式不再有效，客户端将拥有完全的读写权限。

* **debug(boolean)** - 若为 `true` ，将启动调试输出安全规则信息。通常在生产环境中，应该把它设置为false。

###1. JWT Sample

```java
Map<String, Object> payload = new HashMap<String, Object>();
payload.put("uid", "123");
JsonWebToken jwt = new JsonWebToken.Builder()
                .signer("HS256", "OPeJAbqF07a")
                .notBefore(new Date())
                .subject("oauth")
                .issuer("net.tiny")
                .audience("user@net.tiny")
                .jti(true)
                .build(payload);
String token = jwt.token();
```


##More Detail, See The Samples

---
Email   : wuweibg@gmail.com
