## Tiny Service: 一个基于Web系统精简的微服框架
## 设计目的
 - 内置HTTP服务器引擎是com.sun.net.httpserver.HttpsServer。
 - 提供极简的HTTP和HTTPS的MicroService模式。
 - 支持Endpoints编程。
 - 使用Tiny boot包进行服务器配置。
 - 内置标准Web服务器以及控制接口(API)。
 - 支持文件系统快速缓存(Lur Cache)。
 - 提供精简的HTTP客户端Java类。

##Usage

###1. Simple Run
```java
java net.tiny.boot.Main --verbose
```


###2. Application configuration file with profile
```properties
Configuration file : application-{profile}.[yml, json, conf, properties]

main = ${launcher}
daemon = true
executor = ${pool}
callback = ${services}
pool.class = net.tiny.service.PausableThreadPoolExecutor
pool.size = 2
pool.max = 10
pool.timeout = 1
launcher.class = net.tiny.ws.Launcher
launcher.builder.bind = 192.168.1.1
launcher.builder.port = 80
launcher.builder.backlog = 10
launcher.builder.stopTimeout = 1
launcher.builder.executor = ${pool}
launcher.builder.handlers = ${resource}, ${health}, ${sample}
services.class = net.tiny.service.ServiceLocator
resource.class = net.tiny.ws.ResourceHttpHandler
resource.path = /
resource.filters = ${logger}
resource.paths = img:/home/img, js:/home/js, css:/home/css, icon:/home/icon
health.class = net.tiny.ws.VoidHttpHandler
health.path = /health
health.filters = ${logger}
sample.class = your.SimpleJsonHandler
sample.path = /json
sample.filters =  ${logger}, ${snap}, ${params}
logger.class = net.tiny.ws.AccessLogger
logger.format = COMBINED
logger.file = /var/log/http-access.log
params.class = net.tiny.ws.ParameterFilter
snap.class = net.tiny.ws.SnapFilter
```


###3. Sample MicroService java
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

##More Detail, See The Samples

---
Email   : wuweibg@gmail.com
