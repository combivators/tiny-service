## Tiny Service: 一个基于Web系统精简的微服框架
## 设计目的
 - 内置HTTP服务器引擎是com.sun.net.httpserver.HttpsServer。
 - 提供极简的HTTP(S)的MicroService模式。
 - 支持Endpoint编程。
 - 使用Tiny boot包进行服务器配置。
 - 内置标准Web服务器以及控制接口(API)。
 - 支持文件系统快速缓存(Lur Cache)。
 - 提供精简的HTTP客户端Java类。
 - 提供微服之间处理异步消息流的框架。
 - 提供用于微服认证的JSON Web Token(JWT)生成器。

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
 - Sample1

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

 - Sample2

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

TokenGenerator tokenGenerator = new TokenGenerator.Builder("<YOUR_SECRET_KEY>")
                .expires(new Date())
                .notBefore(new Date())
                .admin(true)
                .build();
String token = tokenGenerator.createToken(payload);
```


##More Detail, See The Samples

---
Email   : wuweibg@gmail.com
