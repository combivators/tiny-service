package net.tiny.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.tiny.boot.ApplicationContext;
import net.tiny.boot.Main;
import net.tiny.ws.Launcher;


public class ServiceContextTest {

    @BeforeAll
    public static void beforeAll() throws Exception {
    LogManager.getLogManager()
        .readConfiguration(Thread.currentThread().getContextClassLoader().getResourceAsStream("logging.properties"));
    //Comment out SLF4JBridgeHandler to show exception trace when tomcat start failed
    //Bridge the output of java.util.logging.Logger
//    org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
//    org.slf4j.bridge.SLF4JBridgeHandler.install();
//    LOGGER.log(Level.INFO, String.format("[REST] %s() SLF4J Bridge the output of JUL",
//            Bootstrap.class.getSimpleName()));
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

        ServiceContext locator = ServiceLocator.getInstance();
        System.out.println("ServiceContext: " + locator.toString());

        Launcher launcher = context.getBootBean(Launcher.class);
        assertNotNull(launcher);
        launcher = locator.lookup("launcher", Launcher.class);
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
}
