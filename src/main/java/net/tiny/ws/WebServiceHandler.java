package net.tiny.ws;

import java.util.List;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpHandler;

public interface WebServiceHandler extends HttpHandler, Constants {
    String path();
    WebServiceHandler path(String path);

    boolean hasFilters();
    List<Filter> getFilters();
    WebServiceHandler filters(List<Filter> filters);
    WebServiceHandler filter(Filter filter);

    boolean isAuth();
    Authenticator getAuth();
    WebServiceHandler auth(Authenticator auth);

    <T> T lookup(Class<T> type);
    <T> T lookup(String name, Class<T> type);
}
