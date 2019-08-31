package net.tiny.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

public class ClassFinderTest {
    static Set<Class<? extends Annotation>> TARGTE_ANNOTATIONS = new HashSet<>();

    @Test
    public void testOS() throws Exception {
        System.out.println("OSX: " + ClassFinder.OSX);
        System.out.println("OSX: " + ClassFinder.WINDOWS);
        System.out.println("OSX: " + ClassFinder.UNIX);
    }

    @Test
    public void testFindAllAnnotated() throws Exception {
        String namePattern = "net.tiny.*, !net.tiny.service.ServiceContext";
        Patterns patterns = Patterns.valueOf(namePattern);
        assertFalse(patterns.vaild("net.tiny.service.ServiceContext"));

        ClassFinder.setLoggingLevel(Level.INFO);
        ClassFinder classFinder = new ClassFinder(ClassFinder.createClassFilter(patterns));

        Set<Class<?>> classes = classFinder.findAll();
        assertFalse(classes.isEmpty());
        System.out.println("ClassFinder.findAll(): " + classes.size());
/*
        List<Class<?>> list = classFinder.findAnnotatedClasses(Singleton.class);
        assertTrue(list.isEmpty());
        System.out.println("ClassFinder.findAnnotatedClasses(Singleton.class): " + list.size());

        list = classFinder.findAnnotatedClasses(InterceptorBinding.class);
        assertFalse(classes.isEmpty()); //net.ec.cache.CacheConsumer
        System.out.println("ClassFinder.findAnnotatedClasses(InterceptorBinding.class): " + list.size());

        list = classFinder.findAnnotatedClasses(Interceptor.class);
        assertFalse(list.isEmpty());
        System.out.println("ClassFinder.findAnnotatedClasses(Interceptor.class): " + list.size());

        list = classFinder.findAnnotatedClasses(Transactional.class);
        assertFalse(list.isEmpty());
        System.out.println("ClassFinder.findAnnotatedClasses(Transactional.class): " + list.size());
        for(Class<?> c : list) { System.out.println(c.getName()); }

        List<Method> methods = classFinder.findAnnotatedMethods(AroundInvoke.class);
        assertFalse(methods.isEmpty());
        System.out.println("ClassFinder.findAnnotatedMethods(AroundInvoke.class): " + methods.size());

        List<Field> fields = classFinder.findAnnotatedFields(Inject.class);
        assertFalse(fields.isEmpty());
        System.out.println("ClassFinder.findAnnotatedFields(Inject.class): " + fields.size());
*/
    }

    @Test
    public void testFindAnnotatedClass() throws Exception {
        ClassFinder.setLoggingLevel(Level.INFO);
        String namePattern = "net[.]tiny[.].*, !net[.]tiny[.]text[.].*";
        ClassFinder classFinder = new ClassFinder(new AnnotatedFilter(namePattern, TARGTE_ANNOTATIONS));
        Set<Class<?>> classes = classFinder.findAll();
        System.out.println("ClassFinder.findAll() " + classes.size()); //42

        List<Class<?>> interfaces = classFinder.findAnnotatedInterfaces();
        System.out.println("ClassFinder.findAnnotatedInterfaces() " + interfaces.size()); //11
        //assertFalse(interfaces.isEmpty());

        interfaces = classFinder.findAnnotatedSingleInterfaces();
        System.out.println("ClassFinder.findAnnotatedSingleInterfaces() " + interfaces.size()); //11
        //assertFalse(interfaces.isEmpty());
/*
        for(Class<?> interfaceType : interfaces) {
            System.out.println(" # interfaces " + interfaceType.getName());
        }
*/
/*
        List<Class<?>> impls = classFinder.findImplementations(Bank.class);
        assertEquals(3, impls.size());
        System.out.println("ClassFinder.findImplementations(Bank.class) " + impls.size());

        List<Class<? extends Annotation>> qualifiers = new ArrayList<Class<? extends Annotation>>();
        qualifiers.add(Dependent.class);
        impls = classFinder.findAnnotatedImplementations(Bank.class, qualifiers);
        assertEquals(2, impls.size());
        System.out.println("ClassFinder.findAnnotatedImplementations(Bank.class, qualifiers) " + impls.size());
*/
        /*
        for(Class<?> impl : impls) {
            System.out.println(" # implementation " + impl.getName());
        }
        */
/*
        impls = classFinder.findImplementations(InterfaceTest.WannabeSingleton.class);
        assertEquals(1, impls.size());
        System.out.println("ClassFinder.findImplementations(InterfaceTest.WannabeSingleton.class) " + impls.size());

        impls = classFinder.findAnnotatedInnerClasses();
        System.out.println("ClassFinder.findAnnotatedInnerClasses() " + impls.size());

        impls = classFinder.findAnnotatedInnerClasses(InjectPropertyTest.class);
        assertEquals(4, impls.size());
        System.out.println("ClassFinder.findAnnotatedInnerClasses(IInjectPropertyTest.class) " + impls.size());
*/
        /*
        for(Class<?> impl : impls) {
            System.out.println(" # implementation " + impl.getName());
        }
        */
/*
        impls = classFinder.findAnnotatedInnerClasses(SingletonTest.class);
        assertEquals(2, impls.size());
        System.out.println("ClassFinder.findAnnotatedInnerClasses(SingletonTest.class) " + impls.size());
*/
    }
/*
    @Test
    public void testFindEntityClasses() throws Exception {
        String namePattern = "!net.ec.dao.*, net.shopxx.*";
        Patterns patterns = Patterns.valueOf(namePattern);
        assertTrue(patterns.vaild("net.shopxx.entity.Ad"));

        ClassFinder.setLoggingLevel(Level.INFO);
        ClassFinder classFinder = new ClassFinder(new TestClassFilter(namePattern));

        List<Class<?>> classes = classFinder.findAnnotatedClasses(javax.persistence.Entity.class);
        for (Class<?> c : classes) {
            System.out.println(c.getName());
        }
    }
*/
//    @Test
//    public void testFindRestClasses() throws Exception {
//        String namePattern = "!net.ec.rest.*, net.shopxx.controller.*";
//
//        ClassFinder.setLoggingLevel(Level.INFO);
//        ClassFinder classFinder = new ClassFinder(ClassFinder.createClassFilter(namePattern));
//
//        List<Class<?>> classes = classFinder.findAnnotatedClasses(javax.ws.rs.Path.class);
//        for (Class<?> c : classes) {
//            System.out.println(c.getName());
//        }
//    }

    @Test
    public void testPatterns() throws Exception {
        Patterns patterns = new Patterns(null, "java[.].*, javax[.].*");
        String className = "javax.ws.rs.core.Cookie";
        assertTrue(patterns.invaild(className));
        patterns = new Patterns(null, "net[.]ec[.]di[.]sample1[.].*");
        className = "net.ec.di.sample1.World";
        assertTrue(patterns.invaild(className));
    }

    @Test
    public void testClassForName() throws Exception {
        Patterns patterns = new Patterns(null, "java[.].*, javax[.].*");
        String className = "javax.ws.rs.core.Cookie";
        assertTrue(patterns.invaild(className));
        try {
            Class<?> targetClass = Class.forName(className);
            fail(targetClass.toString());
        } catch (Throwable error) {
            //error.printStackTrace();
            //assertTrue(error instanceof ExceptionInInitializerError);
        }
    }

/*
    @Test
    public void testScanAll() throws Exception {
        ClassFinder.setLoggingLevel(Level.INFO);
        ClassFinder classFinder = new ClassFinder();
        List<Class<?>> scopedClasses = classFinder.findAnnotatedClasses(ApplicationScoped.class);
        System.out.println("ClassFinder.findAnnotatedClasses(): " + scopedClasses.size());
        for(Class<?> c : scopedClasses) {
            System.out.println(" > " + c.getName());
        }
        System.out.println();
        System.out.println("----------------------------------------");

        classFinder = new ClassFinder();
        Set<Class<?>> classSet = classFinder.findAll();
        for(Class<?> classType : classSet) {
            if(classType.isAnnotation()) {
                continue;
            }
            List<Class<?>> interfaces = classFinder.getInterfaces(classType);
            for(Class<?> interfaceType : interfaces) {
                System.out.println(String.format("registerInterface : '%1$s' - '%2$s'" , interfaceType.getName(), classType.getName()));
            }
            String name = classFinder.getNamedValue(classType);
            if(null != name) {
                if(name.isEmpty()) {
                    name = classType.getSimpleName(); //TODO
                }
                List<Class<?>> supers = classFinder.getSuperClasses(classType);
                for(Class<?> parentType : supers) {
                    System.out.println(String.format("registerResource : '%1$s' - @Named '%3$s' (%2$s)", parentType, classType, name));
                }
            }
        }
        //bindProvider(Context.class, context);
        //bindProvider(Registry.class, registry);
        //bindResource(Binder.class, EasyDI.class, "binder"); //TODO

    }
*/


/*

    static {
        TARGTE_ANNOTATIONS.add(javax.inject.Singleton.class);
        TARGTE_ANNOTATIONS.add(javax.inject.Named.class);
        TARGTE_ANNOTATIONS.add(javax.jws.WebService.class);
        // TODO Need CDI
        TARGTE_ANNOTATIONS.add(javax.enterprise.context.Dependent.class);
        TARGTE_ANNOTATIONS.add(javax.enterprise.inject.Model.class);

        // TODO Need JAX-RS
        TARGTE_ANNOTATIONS.add(javax.ws.rs.Path.class);
        TARGTE_ANNOTATIONS.add(javax.ws.rs.ext.Provider.class);
        // TODO Need JTA
        TARGTE_ANNOTATIONS.add(javax.transaction.Transactional.class);

        // TODO Need JFS
        TARGTE_ANNOTATIONS.add(javax.faces.bean.ManagedBean.class);
        TARGTE_ANNOTATIONS.add(javax.faces.bean.RequestScoped.class);
        TARGTE_ANNOTATIONS.add(javax.faces.bean.ViewScoped.class);
        TARGTE_ANNOTATIONS.add(javax.faces.bean.SessionScoped.class);
        TARGTE_ANNOTATIONS.add(javax.faces.bean.ApplicationScoped.class);
        // TODO Need Interceptor
        TARGTE_ANNOTATIONS.add(javax.interceptor.InterceptorBinding.class);

        TARGTE_ANNOTATIONS = Collections.unmodifiableSet(TARGTE_ANNOTATIONS);
        TARGTE_ANNOTATIONS = Collections.synchronizedSet(TARGTE_ANNOTATIONS);
    }
*/
    static class AnnotatedFilter implements ClassFinder.Filter {

        private final Set<Class<? extends Annotation>> targets;
        private Patterns patterns;

        public AnnotatedFilter(String pattern, final Set<Class<? extends Annotation>> targets) {
            this.patterns = Patterns.valueOf(pattern);
            this.targets = targets;
        }

        public AnnotatedFilter(Patterns patterns, final Set<Class<? extends Annotation>> targets) {
            this.patterns = patterns;
            this.targets = targets;
        }

        /**
         * @see ClassFinder.Filter#isTarget(String)
         */
        @Override
        public boolean isTarget(String className) {
            if(this.patterns.vaild(className)) {
                return true;
            }
            return false;
        }

        /**
         * @see ClassFinder.Filter#isTarget(Class)
         */
        @Override
        public boolean isTarget(Class<?> targetClass) {
            Annotation[] annotations = targetClass.getAnnotations();
            for (Annotation annotation : annotations) {
                if(isAnnotatedInstance(annotation)) {
                    return true;
                }
            }
            return isSuperAnnotated(targetClass);
        }

        private boolean isAnnotatedInstance(Annotation annotation) {
            for(Class<? extends Annotation> a : targets) {
                if(a.isInstance(annotation)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isSuperAnnotated(Class<?> targetClass) {
            Class<?> superClass = targetClass.getSuperclass();
            Class<?>[] interfaceTypes = null;
            if(!targetClass.isInterface()) {
                interfaceTypes = targetClass.getInterfaces();
            }
            if(null == superClass && (null == interfaceTypes || interfaceTypes.length == 0)) {
                //Not super class and has not interface
                return false;
            }
            for(Class<?> interfaceType : interfaceTypes) {
                if(isTarget(interfaceType)) {
                    return true;
                }
            }
            return isTarget(superClass);
        }
    }
}
