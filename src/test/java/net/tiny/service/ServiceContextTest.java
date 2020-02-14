package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogManager;

import javax.annotation.Resource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.boot.ApplicationContext;
import net.tiny.boot.Main;
import net.tiny.config.Configuration;
import net.tiny.config.ConfigurationHandler;
import net.tiny.config.ContextHandler;
import net.tiny.service.ServiceContext;
import net.tiny.service.ClassFinder;
import net.tiny.service.ClassHelper;
import net.tiny.service.Patterns;
import net.tiny.ws.Launcher;


public class ServiceContextTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
        LogManager.getLogManager()
            .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    }

    @Test
    public void testFindClasses() throws Exception {
        String namePattern = "net.tiny.*, !net.tiny.boot.*";
        Patterns patterns = Patterns.valueOf(namePattern);
        assertFalse(patterns.vaild("net.tiny.boot.Main"));

        ClassFinder.setLoggingLevel(Level.INFO);
        ClassFinder classFinder = new ClassFinder(ClassFinder.createClassFilter(patterns));

        Set<Class<?>> classes = classFinder.findAll();
        assertFalse(classes.isEmpty());
        System.out.println("ClassFinder.findAll(): " + classes.size());

        Set<Class<?>> ifs = classFinder.findAllInterfaces();
        assertFalse(ifs.isEmpty());
        System.out.println("ClassFinder.findAllInterfaces() " + ifs.size());

        Set<Class<?>> ss = classFinder.findAllWithInterface(false);
        assertFalse(ss.isEmpty());
        System.out.println("ClassFinder.findAllWithInterface() " + ss.size());
        ss.stream().forEachOrdered( i -> System.out.println(i.getName()));

        List<Class<?>> impls = classFinder.findImplementations(ServiceContext.class);
        assertEquals(1, impls.size());
        System.out.println("ClassFinder.findImplementations(ServiceLocator.class) " + impls.size());
        Class<?> type = impls.get(0);
        System.out.println(type.getName());

    }


    @Test
    public void testFindServices() throws Exception {
        String pattern = "net.tiny.*, !net.tiny.boot.*";
        ClassFinder.setLoggingLevel(Level.INFO);
        ClassFinder classFinder = new ClassFinder(ClassFinder.createClassFilter(pattern));

        Set<Class<?>> interfaces = classFinder.findAllInterfaces();
        Set<Class<?>> services = new HashSet<>();
        for (Class<?> i : interfaces) {
            List<Class<?>> list = classFinder.findImplementations(i);
            for (Class<?> s : list) {
                if (!ClassHelper.isInnerClass(s)) {
                    String sid = i.getSimpleName();
                    System.out.println(String.format("'%s' - %s", sid, s.getSimpleName()));
                    if (!services.contains(s)) {
                        services.add(s);
                    }
                }
            }
        }
    }

    @Test
    public void testApplicationContext() throws Exception {
        String[] args = new String[] {"-v", "-p", "test"};
        //asynchronous
        ApplicationContext context = new Main(args).run(false);

        System.out.println("ApplicationContext: " + context.toString());

        ServiceContext serviceContext = context.getBean("service", ServiceContext.class);
        assertNotNull(serviceContext);
        assertNotNull(serviceContext.lookup("launcher", Launcher.class));

        Launcher launcher = context.getBootBean(Launcher.class);
        assertNotNull(launcher);


        Thread.sleep(1000L);
        assertTrue(launcher.isStarting());

        Thread.sleep(100L);
        launcher.stop();

        Thread.sleep(1000L);
        Future<Integer> result = context.getFuture();
        assertNotNull(result);
        assertEquals(0, result.get().intValue());
    }

    static final String LS = System.getProperty("line.separator");

    @Test
    public void testInjectServiceContext() throws Exception {
        ServiceLocator context = new ServiceLocator();

        String conf =
          "main: ${app.sample}, ${app.test}" + LS
        + "app:" + LS
        + "  sample: " + LS
        + "    class: net.tiny.service.ServiceContextTest$SampleBean" + LS
        + "  test:" + LS
        + "    class: net.tiny.service.ServiceContextTest$TestBean" + LS
        + "  handler:" + LS
        + "    class: net.tiny.service.ServiceContextTest$SampleHandler" + LS
        + LS;
        ByteArrayInputStream bais = new ByteArrayInputStream(conf.getBytes());

        Collector collector = new Collector();
        ConfigurationHandler handler = new ConfigurationHandler();
        handler.setListener(collector);
        handler.parse(bais, ContextHandler.Type.YAML, true);
        Configuration configuration = handler.getConfiguration();
        configuration.remains();

        context.accept(new Callable<Properties>() {
            @Override
            public Properties call() {
                // Setup service locator properties;
                Properties services = new Properties();
                services.put("config", configuration);
                services.put("main", this);
                services.put("PID", 12345L);
                for (String key : collector.keys()) {
                    services.put(key, collector.get(key));
                }
                collector.collection.clear();
                return services;
            }
        });

        SampleBean sb = context.lookup(SampleBean.class);
        assertNotNull(sb);
        assertNotNull(sb.context);
        assertSame(context, sb.context);
        TestBean tb = context.lookup(TestBean.class);
        assertNotNull(tb);
        assertNotNull(tb.context);
        assertSame(context, tb.context);

        SampleHandler sh = context.lookup(SampleHandler.class);
        assertNotNull(sh);
        assertNotNull(sh.context);
        assertSame(context, sh.context);
    }

    static class Collector implements ContextHandler.Listener {
        final Map<String, Object> collection = new HashMap<>();

        @Override
        public void created(Object bean, Class<?> beanClass) {
        }

        @Override
        public void parsed(String type, String resource, int size) {
        }

        @Override
        public void cached(String name, Object value, boolean config) {
            if (!config) {
                collection.put(name, value);
            }
        }
        public Set<String> keys() {
            return collection.keySet();
        }
        public Object get(String name) {
            return collection.get(name);
        }
    }

    public static class SampleBean {
        @Resource
        ServiceContext context;

    }

    public static class TestBean {
        @Resource
        ServiceContext context;
    }

    static abstract class AbstractSample {
        @Resource
        protected ServiceContext context;

    }
    static abstract class SampleBase extends AbstractSample {}

    public static class SampleHandler extends SampleBase {
    }

}
