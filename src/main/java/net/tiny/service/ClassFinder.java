package net.tiny.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Supplier;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ClassFinder {

    private static Logger LOGGER = Logger.getLogger(ClassFinder.class.getName());

    public static interface Filter {
        boolean isTarget(Class<?> classType);
        boolean isTarget(String className);
    }

    public static enum DiscoveryMode {
        none, annotated, all;
    }

    public static final boolean OSX = "Mac OS X".equals(System.getProperty("os.name"));
    public static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    public static final boolean UNIX = (!OSX && !WINDOWS);

    private static Level loggingLevel = Level.FINE;
    protected static boolean verbose = false;

    private static class ClassPatternFilter implements Filter {
        private final Patterns patterns;
        private ClassPatternFilter(String patterns) {
            this.patterns = Patterns.valueOf(patterns);
        }

        private ClassPatternFilter(Patterns patterns) {
            this.patterns = patterns;
        }

        @Override
        public boolean isTarget(Class<?> targetClass) {
            return true;
        }

        @Override
        public boolean isTarget(String className) {
            return this.patterns.vaild(className);
        }
    };

    private static class PackageFilter implements Filter {
        final HashSet<String> packages;

        public PackageFilter(final String... packages) {
            this.packages = new HashSet<>();
            for (String pkg : packages) {
                this.packages.add(pkg);
            }
        }

        public PackageFilter(final Collection<String> packages) {
            this.packages = new HashSet<>();
            this.packages.addAll(packages);
        }

        @Override
        public boolean isTarget(Class<?> classType) {
            return true;
        }

        @Override
        public boolean isTarget(String className) {
            for (String prefix : packages) {
                if (className.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static void setLoggingLevel(String level) {
        setLoggingLevel(Level.parse(level));
    }

    public static void setLoggingLevel(Level level) {
        loggingLevel = level;
    }

    public static Filter createClassFilter(String patterns) {
        return new ClassPatternFilter(patterns);
    }
    public static Filter createClassFilter(Patterns patterns) {
        return new ClassPatternFilter(patterns);
    }
    public static Collection<URL> getUrls() {
        return getUrls(true, null);
    }

    public static Collection<URL> getUrls(final String exclude) {
        return getUrls(true, exclude);
    }

    public static Collection<URL> getUrls(boolean discovery, final String exclude) {
        final UrlSet urlSet = getUrlSet(exclude);
        if(discovery) {
            //BeansXml.BeanDiscoveryMode mode = findDiscoveryMode(urlSet); //TODO
            DiscoveryMode mode = DiscoveryMode.annotated;
            return getUrls(urlSet, mode);
        } else {
            return urlSet.getUrls();
        }
    }

/*
    private static BeansXml fetchBeansXml(UrlSet urlSet) {
        BeansXml.BeanDiscoveryMode mode = BeansXml.BeanDiscoveryMode.none;
        BeansXml xml = null;;
        Set<URL> classPaths = urlSet.getPaths();
        for(URL url : classPaths) {
            try {
                String resource = url.toString() + BeansXml.BEANS_XML;
                BeansXml bx = BeansXml.valueOf(resource);
                if(bx.getBeanDiscoveryMode().ordinal() > mode.ordinal()) {
                    mode = bx.getBeanDiscoveryMode();
                    xml = bx;
                }
                LOGGER.log(loggingLevel, String.format("[ClassFinder] - Found discovery mode '%1$s' from '%2$s' ",
                        bx.getBeanDiscoveryMode(), resource));
            } catch (Exception ex) {
                // Not found ignore
            }
        }
        return xml;
    }

    private static BeansXml.BeanDiscoveryMode findDiscoveryMode(UrlSet urlSet) {
        BeansXml.BeanDiscoveryMode mode = BeansXml.BeanDiscoveryMode.none;
        BeansXml beansXml = fetchBeansXml(urlSet);
        if(null != beansXml) {
            mode = beansXml.getBeanDiscoveryMode();
        }
        return mode;
    }
*/
    private static Collection<URL> getUrls(UrlSet urlSet, DiscoveryMode mode) {
        LinkedHashSet<URL> paths = new LinkedHashSet<>();
        Set<URL> jars;
        switch(mode) {
        case none:
            break;
        case annotated:
            paths.addAll(urlSet.getPaths());
            jars = urlSet.getJars();
            for(URL url : jars) {
                try {
                    //TODO
                    //String resource = url.toString() + BeansXml.BEANS_XML;
                    //BeansXml beansXml = BeansXml.valueOf(resource);
                    //if(!beansXml.getBeanDiscoveryMode().equals(BeansXml.BeanDiscoveryMode.none)) {
                        paths.add(url);
                    //}
                    //LOGGER.log(loggingLevel, String.format("[ClassFinder] - Found '%1$s' discovery mode '%2$s'",
                    //        resource, beansXml.getBeanDiscoveryMode()));
                } catch (Exception ex) {
                    // Not found ignore
                }
            }
            break;
        case all:
            paths.addAll(urlSet.getUrls());
            break;
        }
        return paths;
    }

    private static UrlSet getUrlSet(final String pattern) {
        UrlSet urlSet = new UrlSet();
        if (pattern != null) {
            urlSet = urlSet.matching(pattern);
        }
        return urlSet;
    }

    private Patterns excludePatterns =
            new Patterns("java[.].*, javax[.].*, org[.]omg[.].*, org[.]w3c[.]dom[.].*, org[.]eclipse[.]jdt[.].*, .*[.]package-info$");

    private DiscoveryMode discoveryMode = DiscoveryMode.all;
    //private BeansXml.BeanDiscoveryMode discoveryMode = BeansXml.BeanDiscoveryMode.all;
    //private BeansXml beansXml = null;
    private ClassLoader classLoader;
    private Filter filter = null;
    private Collection<URL> targetUrls = null;
    // Cache target class type
    protected final Set<Class<?>> classTypes = new HashSet<>();
    // Cache interface class types
    protected final Map<Class<?>, List<Class<?>>> annotated = new HashMap<>();

    /**
     * Creates a ClassFinder that will search the urls from the default class path
     * scanning the urls in the patterns.
     *
     * @param pattern
     *            Pattern of class path for scan
     * @param filter
     *            filter of source class pattern to include or exclude from scanning
     */
    public ClassFinder(String pattern, Filter filter) {
        this(Thread.currentThread().getContextClassLoader(), getUrlSet(pattern), filter);
    }


    /**
     * Creates a ClassFinder that will search the urls in the specified
     * classloader excluding the urls in the classloader's parent.
     *
     * To include the parent classloader, use:
     *
     * new ClassFinder(classLoader, false);
     *
     * To exclude the parent's parent, use:
     *
     * new ClassFinder(classLoader, classLoader.getParent().getParent());
     *
     * @param classLoader
     *            source of classes to scan
     */
    public ClassFinder(final Filter filter) {
        this(Thread.currentThread().getContextClassLoader(), getUrlSet(null), filter);
    }

    public ClassFinder(final ClassLoader classLoader) throws Exception {
        this(classLoader, getUrlSet(null), null);
    }

    public ClassFinder(final String patterns)  {
        this(Thread.currentThread().getContextClassLoader(), getUrlSet(null), createClassFilter(patterns));
    }

    public ClassFinder(final Collection<String> packages) {
        this(Thread.currentThread().getContextClassLoader(), getUrlSet(null), new PackageFilter(packages));
    }

    public ClassFinder(final String... packages) {
        this(Thread.currentThread().getContextClassLoader(), getUrlSet(null), new PackageFilter(packages));
    }

    public ClassFinder() {
        this(Thread.currentThread().getContextClassLoader(), getUrlSet(null), null);
    }

    public ClassFinder(Class<?>... classes) {
        this(null, Arrays.asList(classes));
    }

    public ClassFinder(final Filter filter, Class<?>... classes) {
        this(filter, Arrays.asList(classes));
    }

//    public ClassFinder(final ClassLoader classLoader, final URL url, final Filter filter) {
//        this(classLoader, Arrays.asList(url), filter);
//    }

//    public ClassFinder(final ClassLoader classLoader, final Collection<URL> urls, final Filter filter) {
//        this.classLoader = classLoader;
//        this.filter = filter;
//        this.discoveryMode = DiscoveryMode.annotated;
//        this.targetUrls = urls;
//        load(true);
//    }

    private ClassFinder(final ClassLoader classLoader, final UrlSet urlSet, final Filter filter) {
        //this.classLoader = Thread.currentThread().getContextClassLoader();
        this.classLoader = classLoader;
        this.filter = filter;
        /*
        this.beansXml = fetchBeansXml(urlSet);
        if(null != this.beansXml) {
            this.discoveryMode = this.beansXml.getBeanDiscoveryMode();
            this.excludePatterns.getInclude().addAll(this.beansXml.getPatterns());
        }
        */
        this.targetUrls = getUrls(urlSet, this.discoveryMode);
        long st = System.currentTimeMillis();
        this.targetUrls.stream()
                .forEach(url -> load(true, url));
        long eta = System.currentTimeMillis() - st;
        LOGGER.log(loggingLevel, String.format("[ClassFinder] Load %d url(s) ETA:%dms", targetUrls.size(), eta));
    }

    public ClassFinder(final Filter filter, Collection<Class<?>> classes) {
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.filter = filter;
        this.discoveryMode = DiscoveryMode.annotated;
        for (Class<?> c : classes) {
            try {
                readClassDef(c);
            } catch (NoClassDefFoundError e) {
                throw new NoClassDefFoundError("Could not fully load class: " + c.getName() + "\n due to:"
                        + e.getMessage() + "\n in classLoader: \n" + c.getClassLoader());
            }
        }
    }


    synchronized void load(boolean force, URL location) {
        LOGGER.info(String.format("[ClassFinder] - load '%s'", location.toString())); //TODO
        List<String> classNames = new ArrayList<String>();
        if (location.getProtocol().equals("jar")) {
            try {
                List<String> targets = jar(location);
                if (!targets.isEmpty()) {
                    LOGGER.log(loggingLevel, String.format("[ClassFinder] - Found %d classe(s) on '%s'",
                                    targets.size(), location.toString()));
                    classNames.addAll(targets);
                }
            } catch (IOException ex) {
                LOGGER.warning(String.format("[ClassFinder] - Read '%s' error: %s'", location, ex.getMessage()));
            }
        } else if (location.getProtocol().equals("file")) {
            try {
                final String external = location.toExternalForm();
                //if (!external.contains("tiny") || external.contains("tiny-dic")) {
                //	return; //TODO
                //}
                // See if it's actually a jar
                URL jarUrl = new URL("jar", "", external + "!/");
                JarURLConnection juc = (JarURLConnection) jarUrl.openConnection();
                juc.getJarFile();
                List<String> targets = jar(jarUrl);
                if (!targets.isEmpty()) {
                    LOGGER.log(loggingLevel, String.format("[ClassFinder] - Found %d classe(s) on '%s'",
                                    targets.size(), jarUrl.toString()));
                    classNames.addAll(targets);
                }
            } catch (IOException ex) {
                // See if it's local class path
                List<String> targets = file(location);
                if (!targets.isEmpty()) {
                    LOGGER.log(loggingLevel, String.format("[ClassFinder] - Found %d classe(s) on '%s'",
                                    targets.size(), location.toString()));
                    classNames.addAll(targets);
                }
            }
        }

        for (String className : classNames) {
            readClassDef(className);
        }
    }


    public Filter getFilter() {
        return this.filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Set<Class<?>> find(final Collection<String> packages) {
        return find(new PackageFilter(packages));
    }

    public Set<Class<?>> find(final String... packages) {
        return find(new PackageFilter(packages));
    }

    public Set<Class<?>> find(final Filter classFilter) {
        if (null == classFilter) {
            throw new IllegalArgumentException("Filter is null.");
        }
        Set<Class<?>> classes = new HashSet<>();
        for (Class<?> classType : this.classTypes) {
            if (classFilter.isTarget(classType.getName()) && classFilter.isTarget(classType)) {
                classes.add(classType);
            }
        }
        return classes;
    }
    /**
     * Find real class list that with a annotated class
     * @param annotation The Class own a annotated class
     * @return
     */
    public List<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation) {
        List<Class<?>> classes = new Vector<>();
        for (Class<?> type : this.classTypes) {
            if (type.getDeclaredAnnotation(annotation) != null) {
                classes.add(type);
            }
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    public List<Class<? extends Annotation>> findAnnotatedAnnotations(Class<? extends Annotation> annotation) {
        List<Class<? extends Annotation>> classes = new Vector<>();
        for (Class<?> type : this.classTypes) {
            if (type.getDeclaredAnnotation(annotation) != null && type.isAnnotation()) {
                classes.add((Class<? extends Annotation>) type);
            }
        }
        return classes;
    }

    public List<Class<?>> findAnnotatedImplementations(Class<? extends Annotation> annotation) {
        List<Class<?>> classes = new Vector<>();
        List<Class<? extends Annotation>> annotations = findAnnotatedAnnotations(annotation);
        for (Class<? extends Annotation> type : annotations) {
            List<Class<?>> impls = findAnnotatedClasses(type);
            for(Class<?> impl : impls) {
                if(!classes.contains(impl)) {
                    classes.add(impl);
                }
            }
        }
        return classes;
    }

    public List<Method> findAnnotatedMethods(Class<? extends Annotation> annotation) {
        List<Method> methods = new Vector<>();
        for (Class<?> type : this.classTypes) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(annotation)) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    public List<Constructor<?>> findAnnotatedConstructors(Class<? extends Annotation> annotation) {
        List<Constructor<?>> constructors = new Vector<>();
        for (Class<?> type : this.classTypes) {
            for (Constructor<?> constructor : type.getConstructors()) {
                if (constructor.isAnnotationPresent(annotation)) {
                    constructors.add(constructor);
                }
            }
        }
        return constructors;
    }

    public List<Field> findAnnotatedFields(Class<? extends Annotation> annotation) {
        List<Field> fields = new Vector<>();
        for (Class<?> type : this.classTypes) {
            try {
                for (Field field : type.getDeclaredFields()) {
                    if (field.isAnnotationPresent(annotation)) {
                        fields.add(field);
                    }
                }
            } catch (Throwable err) {
                // Ignore
            }
        }
        return fields;
    }

    public List<Class<?>> findClassesWithAnnotatedField(Class<? extends Annotation> annotation) {
        List<Class<?>> classes = new Vector<>();
        for (Class<?> type : this.classTypes) {
            try {
                for (Field field : type.getDeclaredFields()) {
                    if (field.isAnnotationPresent(annotation)) {
                        classes.add(type);
                    }
                }
            } catch (Throwable err) {
                // Ignore
            }
        }
        return classes;
    }

    @SuppressWarnings("unchecked")
    public <T> Class<? super Supplier<T>> findSupplier(Class<T> classType) {
        for (Class<?> type : this.classTypes) {
            try {
                if (!Supplier.class.isAssignableFrom(type))
                    continue;
                Type[] types = type.getGenericInterfaces();
                for (Type t : types) {
                    if (!(t instanceof ParameterizedType))
                        continue;
                    for (Type a : ((ParameterizedType)t).getActualTypeArguments()) {
                        if(a.equals(classType)) {
                            return (Class<? super Supplier<T>>)type;
                        }
                    }
                }

            } catch (Throwable err) {
                // Ignore
            }
        }
        return null;
    }

    public List<Class<?>> findAnnotatedInterfaces() {
        List<Class<?>> list = new Vector<Class<?>>();
        Set<Class<?>> types = this.annotated.keySet();
        for (Class<?> type : types) {
            list.add(type);
        }
        return list;
    }

    public List<Class<?>> findAnnotatedSingleInterfaces() {
        List<Class<?>> list = new Vector<Class<?>>();
        Set<Class<?>> types = this.annotated.keySet();
        for (Class<?> type : types) {
            if (this.annotated.get(type).size() == 1) {
                list.add(type);
            }
        }
        return list;
    }

    public List<Class<?>> findImplementations(Class<?> interfaceClass) {
        return findAnnotatedImplementations(interfaceClass, null)
                .stream()
                .filter(c -> !Modifier.isAbstract(c.getModifiers()))
                .collect(Collectors.toList());
    }

    public List<Class<?>> findAnnotatedImplementations(Type interfaceType, Collection<Class<? extends Annotation>> qualifiers) {
        List<Class<?>> classes = new Vector<>();
        List<Class<?>> types = this.annotated.get((Class<?>)interfaceType);
        if (!types.isEmpty()) {
            if(null == qualifiers || qualifiers.isEmpty()) {
                classes.addAll(types);
            } else {
                for(Class<?> type : types) {
                    for(Class<? extends Annotation> annotationClass : qualifiers) {
                        if(null != type.getAnnotation(annotationClass)) {
                            classes.add(type);
                            break;
                        }
                    }
                }
            }
        }
        return classes;
    }

    public List<Class<?>> findAnnotatedInnerClasses() {
        return findAnnotatedInnerClasses(null);
    }

    public List<Class<?>> findAnnotatedInnerClasses(Class<?> parentClass) {
        List<Class<?>> classes = new Vector<>();
        for (Class<?> type : this.classTypes) {
            if (type.isMemberClass() || type.isLocalClass()) {
                if (null != parentClass) {
                    if (type.getName().startsWith(parentClass.getName())) {
                        classes.add(type);
                    }
                } else {
                    classes.add(type);
                }
            }
        }
        return classes;
    }

    public Set<Class<?>> findAll() {
        return this.classTypes;
    }

    public Set<Class<?>> findAllInterfaces() {
        Set<Class<?>> ifs = new HashSet<>();
        for (Class<?> c : classTypes) {
            if (c.isInterface() && !ClassHelper.isInnerClass(c) && ClassHelper.hasMethods(c)) {
                ifs.add(c);
            }
        }
        return ifs;
    }

    public Set<Class<?>> findAllWithInterface() {
        return findAllWithInterface(false);
    }

    public Set<Class<?>> findAllWithInterface(boolean inner) {
        Set<Class<?>> ifs = findAllInterfaces();
        Set<Class<?>> impls = new HashSet<>();
        for (Class<?> c : ifs) {
            List<Class<?>> list = findImplementations(c);
            for (Class<?> i : list) {
                if (!impls.contains(i)) {
                    if(inner) {
                        impls.add(i);
                    } else {
                        if (!ClassHelper.isInnerClass(i))
                            impls.add(i);
                    }
                }
            }
        }
        return impls;
    }

    public Collection<URL> findArchives(String resource) throws IOException {
        final List<URL> result = new ArrayList<>();
        final Enumeration<URL> urls = this.classLoader.getResources(resource);
        while (urls.hasMoreElements()) {
            // either a jar or file in a dir
            URL url = urls.nextElement();
            File file = new File(url.getFile());
            if (file.exists()) {
                // navigate on directory above META-INF
                url = file.getParentFile().getParentFile().toURI().toURL();
            } else {
                url = ((JarURLConnection) url.openConnection()).getJarFileURL();
            }
            result.add(url);
        }
        return result;
    }

    public int size() {
        return this.classTypes.size();
    }

    protected URL getResource(String className) {
        return classLoader.getResource(className);
    }

    protected Class<?> loadClass(String className) throws ClassNotFoundException {
        return classLoader.loadClass(className);
    }

    private boolean isTargetClass(String className) {
        if (this.filter != null) {
            return this.filter.isTarget(className);
        } else {
            if (excludePatterns.vaild(className)) {
                return false;
            }
            return true;
        }
    }

    private boolean isTargetClass(Class<?> type) {
        if (this.filter != null) {
            return this.filter.isTarget(type);
        } else {
            if (excludePatterns.vaild(type.getName())) {
                return false;
            }
            boolean ok = false;
            switch(discoveryMode) {
            case annotated:
                ok = isAnnotatedClass(type);
                break;
            case all:
                ok = true;
                break;
            default:
                break;
            }
            return ok;
        }
    }

    private boolean isAnnotatedClass(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        return (annotations != null && annotations.length > 0);
    }

    private List<String> jar(URL location) throws IOException {
        String jarPath = location.getFile();
        if (jarPath.indexOf("!") > -1) {
            jarPath = jarPath.substring(0, jarPath.indexOf("!"));
        }
        URL url = new URL(jarPath);
        InputStream in = url.openStream();
        try {
            JarInputStream jarStream = new JarInputStream(in);
            return jar(jarStream);
        } finally {
            in.close();
        }
    }

    private List<String> jar(JarInputStream jarStream) throws IOException {
        List<String> classNames = new ArrayList<String>();
        JarEntry entry;
        while ((entry = jarStream.getNextJarEntry()) != null) {
            if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                continue;
            }
            String className = entry.getName();
            className = className.replaceFirst(".class$", "");
            if (className.contains("."))
                continue;
            className = className.replace('/', '.');
            if (isTargetClass(className)) {
                classNames.add(className);
            }
        }
        return classNames;
    }

    private List<String> file(URL location) {
        try {
            List<String> classNames = new ArrayList<String>();
            File dir = new File(URLDecoder.decode(location.getPath(), "UTF-8"));
            if (dir.getName().equals("META-INF")) {
                dir = dir.getParentFile(); // Scrape "META-INF" off
            }
            if (dir.isDirectory()) {
                scanDir(dir, classNames, "");
            }
            return classNames;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void scanDir(File dir, List<String> classNames, String packageName) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                scanDir(file, classNames, packageName + file.getName() + ".");
            } else if (file.getName().endsWith(".class")) {
                String name = file.getName();
                name = name.replaceFirst(".class$", "");
                if (name.contains("."))
                    continue;
                String className = packageName + name;
                if (isTargetClass(className)) {
                    classNames.add(className);
                }
            }
        }
    }

    protected void readClassDef(String className) {
        try {
            if (excludePatterns.vaild(className)) {
                return; // Skip this
            }
            readClassDef(Class.forName(className));
        } catch (Throwable ex) {
            LOGGER.log(Level.WARNING,
                    String.format("Find class '%1$s' error: %2$s  from '%3$s'", className, ex.getMessage(), className),
                    ex);
        }
    }

    private void readInterfaceDef(Class<?> interfaceType, Class<?> implementationType) {
        if (excludePatterns.vaild(interfaceType.getName())) {
            return;
        }
        List<Class<?>> list = this.annotated.get(interfaceType);
        if (null != implementationType) {
            if (null == list) {
                list = new LinkedList<>();
                list.add(implementationType);
                this.annotated.put(interfaceType, list);
            } else {
                if (!list.contains(implementationType)) {
                    list.add(implementationType);
                }
            }
        } else {
            if (null == list) {
                list = new LinkedList<>();
                this.annotated.put(interfaceType, list);
            }
        }
    }

    protected void readClassDef(Class<?> type) {
        if (isTargetClass(type)) {
            this.classTypes.add(type);
            if(verbose) {
                LOGGER.log(loggingLevel, String.format("[ClassFinder] - Class: '%1$s'", type.getName()));
            }
            if (!type.isInterface()) {
                Class<?>[] ifs = type.getInterfaces();
                for (Class<?> i : ifs) {
                    readInterfaceDef(i, type);
                }
            } else {
                readInterfaceDef(type, null);
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////

    public static Field getDeclaredField(Class<?> targetClass, String name) throws NoSuchFieldException {
        try {
            return targetClass.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            Class<?> superClass = targetClass.getSuperclass();
            if(null != superClass) {
                return getDeclaredField(superClass, name);
            } else {
                throw e;
            }
        }
    }


}
