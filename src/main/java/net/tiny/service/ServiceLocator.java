package net.tiny.service;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceLocator implements Consumer<Callable<Properties>>, ServiceContext {

    protected static final Logger LOGGER = Logger.getLogger(ServiceLocator.class.getName());

    protected Container container = new Container();
    private Listener listener = null;


    ////////////////////////////////////////
    // Service consumer callback method, will be called by main process.
    @Override
    public void accept(Callable<Properties> callable) {
        try {
            final Properties services = callable.call();
            final SortedSet<String> names = new TreeSet<>();
            services.keySet()
                .stream()
                .forEach(e -> names.add(String.valueOf(e)));

            for (String name : names) {
                Object bean = services.get(name);
                bind(name, bean, true);
            }

            LOGGER.info(String.format("[BOOT] Bound %d service(s) on ServiceContext#%d", services.size(), hashCode()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, String.format("[BOOT] Service consumer callback error : %s",
                    e.getMessage()) ,e);
        }

    }

    protected void setProcessId() {
        // Get proccess id from JMX
        RuntimeMXBean mxRuntime = ManagementFactory.getRuntimeMXBean();
        String name = mxRuntime.getName();
        if (name.matches("\\d+@.*")) {
            Integer processId = Integer.valueOf(name.substring(0,
                    name.indexOf('@')));
            bind(PID, processId, true);
        }
    }

    @Override
    public int getProcessId() {
        if (!exist(PID)) {
            setProcessId();
        }
        return lookup(PID, Integer.class);
    }

    @Override
    public Listener getListener() {
        return listener;
    }

    @Override
    public void setListener(Listener listener) {
        this.listener = listener;
    }


    @Override
    public <T> T lookup(Class<T> classType) {
        if (classType.equals(Container.class)) {
            return classType.cast(container);
        } else if (classType.equals(ServiceContext.class)) {
            return classType.cast(this);
        }
        List<T> list = container.getBeans(classType);
        if(list.isEmpty())
            return null;
        return list.get(0);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T lookup(String name, Class<T> classType) {
        final Object bean;
        if (ThreadSafeContext.existSharedObject(name)) {
            bean = ThreadSafeContext.getInstance().getSharedObject(name);
        } else {
            bean = container.getBean(name);
        }
        if (null != bean && null != classType) {
            if (!classType.isInstance(bean)) {
                throw new RuntimeException(bean.getClass().getName()
                        + " is not instance of '"
                        + classType.getName() + "'");
            }
        }
        return (T) bean;
    }

    @Override
    public Object lookup(String name) {
        if (ThreadSafeContext.existSharedObject(name)) {
            return ThreadSafeContext.getInstance().getSharedObject(name);
        } else {
            return container.getBean(name);
        }
    }

    @Override
    public <T> Collection<T> lookupGroup(Class<T> classType) {
        return container.getBeans(classType);
    }

    @Override
    public boolean exist(String name) {
        return container.contains(name)
                || ThreadSafeContext.existSharedObject(name);
    }

    @Override
    public boolean exist(String name, Class<?> classType) {
        Object bean;
        if (ThreadSafeContext.existSharedObject(name)) {
            bean = ThreadSafeContext.getInstance().getSharedObject(name);
        } else {
            bean = container.getBean(name, classType);
        }
        if (null != bean && null != classType) {
            return classType.isInstance(bean);
        }
        return false;
    }


    //////////////////////////////////////////////////////////
    // Service Register Methods
    @Override
    public void bind(String name, Object target, boolean singleton) {
        if (!singleton) {
            ThreadSafeContext.getInstance().setSharedObject(name, target);
        } else {
            container.setBean(name, target, singleton);
        }
        if (null != listener) {
            listener.bind(name);
        }
    }

    @Override
    public void unbind(String name) {
        if (ThreadSafeContext.existSharedObject(name)) {
            ThreadSafeContext.getInstance().removeSharedObject(name);
        }
        container.removeBean(name);
        if (null != listener) {
            listener.unbind(name);
        }
    }

    @Override
    public boolean isBinding(String name) {
        return exist(name);
    }


    @Override
    public String getType() {
        return "LOCAL";
    }

    @Override
    public String getAddress() {
        return "localhost:-1";
    }


    @Override
    public void destroy() {
        container.destroy();
        ThreadSafeContext.destroy();
        System.gc();
    }


    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ServiceContext@" + hashCode());
        sb.append(" - ");
        sb.append(container.toString());
        return sb.toString();
    }


    public static class ServiceMonitor implements Listener {
        @Override
        public void invoke(Object inst, Method method, Object param) {
            LOGGER.info(" [invoke] - " + inst.getClass().getName() + "."
                    + method.getName() + "('" + param + "')");
        }
        @Override
        public void bind(String name) {
            LOGGER.info(" [bind] - bind '" + name+ "'");
        }

        @Override
        public void unbind(String name) {
            LOGGER.info(" [unbind] - unbind '" + name+ "'");
        }

        @Override
        public void created(String address) {
            LOGGER.info(" [created] - created on '" + address+ "'");
        }

        @Override
        public void debug(String msg) {
            LOGGER.fine(msg);
        }

        @Override
        public void info(String msg) {
            LOGGER.info(msg);
        }

        @Override
        public void warn(String msg, Throwable exception) {
            if(null != exception) {
                LOGGER.log(Level.WARNING, msg, exception);
            } else {
                LOGGER.log(Level.WARNING, msg);
            }
        }

        @Override
        public void error(String msg, Throwable exception) {
            if(null != exception) {
                LOGGER.log(Level.SEVERE, msg, exception);
            } else {
                LOGGER.log(Level.SEVERE, msg);
            }
        }

    }
}
