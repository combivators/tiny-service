package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Set;

public class UrlSetTest  {


    @Test
    public void testAll() throws Exception {
        System.out.println("[testAll]");
        final URL[] originalUrls = new URL[]{
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/.compatibility/14compatibility.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/charsets.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/classes.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/dt.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jce.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jconsole.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jsse.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/laf.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/ui.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/deploy.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/apple_provider.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/dnsns.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/localedata.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/sunjce_provider.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/ext/sunpkcs11.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/plugin.jar!/"),
                new URL("jar:file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home/lib/sa-jdi.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/CoreAudio.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/MRJToolkit.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/QTJSupport.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/QTJava.zip!/"),
                new URL("jar:file:/System/Library/Java/Extensions/dns_sd.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/j3daudio.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/j3dcore.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/j3dutils.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/jai_codec.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/jai_core.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/mlibwrapper_jai.jar!/"),
                new URL("jar:file:/System/Library/Java/Extensions/vecmath.jar!/"),
                new URL("jar:file:/Users/dblevins/.m2/repository/junit/junit/3.8.1/junit-3.8.1.jar!/"),
        };
        UrlSet urlSet = new UrlSet(originalUrls);

        assertEquals(30, urlSet.getUrls().size(), "Urls.size()");

        UrlSet homeSet = urlSet.matching(".*Home.*");
        assertEquals(8, homeSet.getUrls().size(), "HomeSet.getUrls().size()");

        UrlSet junitSet = urlSet.matching(".*junit.*");
        assertEquals(1, junitSet.getUrls().size(), "JunitSet.getUrls().size()");

        UrlSet mergedSet = homeSet.include(junitSet);
        assertEquals(9, mergedSet.getUrls().size(), "MergedSet.getUrls().size()");

        UrlSet filteredSet = urlSet.exclude(".*System/Library.*");
        assertEquals(1, filteredSet.getUrls().size(), "FilteredSet.getUrls().size()");
    }

    @Test
    public void testGetUrlSet() throws Exception {
        System.out.println("[testGetUrls] - ContextClassLoader");
        Set<URL> urls = UrlSet.getUrlSet();
        assertFalse(urls.isEmpty());
        for(URL url : urls) {
            System.out.println(url.toString());
        }
        System.out.println();

        UrlSet urlSet = new UrlSet();
        Set<URL> list = urlSet.getUrls();
        assertFalse(list.isEmpty());
        for(URL url : list) {
            System.out.println(url.toString());
        }
        System.out.println();
    }

    @Test
    public void testGetClassPaths() throws Exception {
        System.out.println("[testGetPaths]");
        String classpath = System.getProperty("java.class.path");
        System.out.println(classpath);
        String[] entries = classpath.split(File.pathSeparator);
        URL[] result = new URL[entries.length];
        for(int i = 0; i < entries.length; i++) {
            result[i] = Paths.get(entries[i]).toAbsolutePath().toUri().toURL();
            System.out.println(result[i] );
        }
    }

    @Test
    public void testFilter() throws Exception {
        final URL[] originalUrls = new URL[]{
                new URL("file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/.compatibility/14compatibility.jar"),
                new URL("file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/charsets.jar"),
                new URL("file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/classes.jar"),
                new URL("file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/dt.jar"),
                new URL("file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jce.jar"),
                new URL("file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jconsole.jar"),
                new URL("file:/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Classes/jsse.jar"),
                new URL("file:/System/Library/Java/Extensions/CoreAudio.jar"),
                new URL("file:/System/Library/Java/Extensions/j3daudio.jar"),
                new URL("file:/System/Library/Java/Extensions/j3dcore.jar"),
                new URL("file:/System/Library/Java/Extensions/j3dutils.jar"),
                new URL("file:/System/Library/Java/Extensions/jai_codec.jar"),
                new URL("file:/System/Library/Java/Extensions/jai_core.jar"),
                new URL("file:/System/Library/Java/Extensions/mlibwrapper_jai.jar"),
                new URL("file:/System/Library/Java/Extensions/vecmath.jar"),
                new URL("file:/Users/dblevins/.m2/repository/junit/junit/3.8.1/junit-3.8.1.jar"),
                new URL("file:/opt/repository/net/tiny/tiny-boot/1.0.0/tiny-boot-1.0.0.jar"),
                new URL("file:/opt/repository/net/tiny/tiny-boot/1.0.0/tiny-service-1.0.0.jar"),
                new URL("file:/opt/repository/net/tiny/tiny-boot/1.0.0/tiny-dic-1.0.0.jar"),
                new URL("file:/workspace/demo/target/classes/"),
        };

        UrlSet urlSet = new UrlSet(originalUrls);
        assertEquals(20, urlSet.getUrls().size(), "Urls.size()");

        UrlSet junitSet = urlSet.matching(".*/junit-.*[.]jar$");
        assertEquals(1, junitSet.getUrls().size());

        UrlSet tinySet = urlSet.matching(".*/classes/, .*/tiny-.*[.]jar, !.*/tiny-dic.*[.]jar");
        assertEquals(3, tinySet.getUrls().size());

    }

}
