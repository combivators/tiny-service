package net.tiny.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.Filter;

public abstract class AbstractWebService implements WebServiceHandler {

    protected static final Logger LOGGER = Logger.getLogger(AbstractWebService.class.getName());

    protected String path;
    protected List<Filter> filters = new ArrayList<>();
    protected Authenticator auth = null;

    @Override
    public String path() {
        return path;
    }

    @Override
    public WebServiceHandler path(String path) {
        this.path = path;
        return this;
    }
    @Override
    public boolean hasFilters() {
        return !filters.isEmpty();
    }
    @Override
    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public WebServiceHandler filters(List<Filter> filters) {
        this.filters = filters;
        return this;
    }

    @Override
    public WebServiceHandler filter(Filter filter) {
        if (!filters.contains(filter)) {
            filters.add(filter);
        }
        return this;
    }

    @Override
    public boolean isAuth() {
        return auth != null;
    }

    @Override
    public Authenticator getAuth() {
        return auth;
    }

    @Override
    public WebServiceHandler auth(Authenticator auth) {
        this.auth = auth;
        return this;
    }

    // javax.xml.ws.Endpoint
    @Override
    public boolean isEndpoint() {
        // Default not support JavaEE endpoint service.
        // Need other provider package on JDK11
        return false;
    }

    @Override
    public <T> T getBinding(Class<T> classType) {
        if(classType.isInstance(this)) {
            // Has HttpHandler interface
            return classType.cast(this);
        } else {
            throw new IllegalArgumentException(String.format("Can not cast '%s' interface from %s.",
                    classType.getSimpleName(), getClass().getSimpleName()));
        }
    }
}
