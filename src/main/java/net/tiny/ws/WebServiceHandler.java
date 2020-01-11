package net.tiny.ws;

import java.util.List;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;

public interface WebServiceHandler  {
    String path();
    WebServiceHandler path(String path);

    boolean hasFilters();
    List<Filter> getFilters();
    WebServiceHandler filters(List<Filter> filters);
    WebServiceHandler filter(Filter filter);

    boolean isAuth();
    Authenticator getAuth();
    WebServiceHandler auth(Authenticator auth);

    // javax.xml.ws.Endpoint
    boolean isEndpoint();
    <T>T getBinding(Class<T> classType);
    void publish(HttpContext serverContext);
}
