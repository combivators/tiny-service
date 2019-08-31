package net.tiny.service;

import java.util.HashMap;
import java.util.logging.Logger;

public final class ThreadSafeContext  {

    private static final Logger LOGGER =
        Logger.getLogger(ThreadSafeContext.class.getName());

    private static ThreadLocal<ThreadSafeContext> THREAD_LOCAL = null;

    public static ThreadLocal<ThreadSafeContext> getThreadLocal() {
        if(null == THREAD_LOCAL) {
            THREAD_LOCAL = new ThreadLocal<ThreadSafeContext>();
        }
        return THREAD_LOCAL;
    }

    public static ThreadSafeContext getInstance() {
        ThreadSafeContext context = getThreadLocal().get();
        if (context == null) {
            context = new ThreadSafeContext();
            LOGGER.finest("[LT] new ThreadLocal : " + context.toString());
            getThreadLocal().set(context);
        }
        return context;
    }

    public static boolean existSharedObject(String name) {
        if(null != THREAD_LOCAL) {
            ThreadSafeContext context = THREAD_LOCAL.get();
            if(null != context) {
                return context.exist(name);
            }
        }
        return false;
    }

    public static void destroy() {
        if(null != THREAD_LOCAL) {
            ThreadSafeContext context = THREAD_LOCAL.get();
            if (context != null) {
                LOGGER.finest("[LT] destroy " + context.toString()
                        + " shared objects : " + context.getAll().size());
                context.finalize();
            } else {
                LOGGER.finest("[LT] destroy empty shared objects.");
            }
            THREAD_LOCAL.remove();
        }
    }

    private HashMap<String, Object> map = new HashMap<String, Object>();

    private ThreadSafeContext() {}

    public boolean exist(String name) {
        return map.containsKey(name);
    }

    public HashMap<String, Object> getAll() {
        return this.map;
    }

    public void setAll(HashMap<String, Object> map) {
        this.map = map;
    }

    public void clear() {
        this.map.clear();
    }

    public void setSharedObject(String name, Object obj) {
        LOGGER.finest("[LT] set shared object " + name + " : " + obj.toString());
        map.put(name, obj);
    }

    public Object getSharedObject(String name) {
        return map.get(name);
    }

    public Object removeSharedObject(String name) {
        LOGGER.finest("[LT] remove shared object " + name );
        Object obj = map.remove(name);
           if(map.isEmpty()) {
               LOGGER.finest("[LT] destroy empty shared objects.");
               map = null;
               THREAD_LOCAL.remove();
           }
           return obj;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String[] keys = map.keySet().toArray(new String[0]);
        sb.append("ThreadLocal@" + hashCode() + "("+ keys.length + ") = {");
        int count = 0;
        for(String key : keys) {
            count++;
            String name	=	key;
            sb. append ("'"+ name + "'");
            if(count<keys.length) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    protected void finalize() {
        if(map != null) {
            map.clear();
            map = null;
        }
    }
}
