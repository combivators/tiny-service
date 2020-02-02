package net.tiny.service;

/**
 * java.version is a system property that exists in every JVM
 * Java 8 or lower: 1.6.0_23, 1.7.0, 1.7.0_80, 1.8.0_211
 * Java 9 or higher: 9.0.1, 11.0.4, 12, 12.0.1
 */
public class RuntimeVersion {

    public static int get() {
        String version = System.getProperty("java.version");
        if(version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if(dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    public static boolean equals(int version) {
        return get() == version;
    }

    public static boolean lower(int version) {
        return get() <= version;
    }

    public static boolean higher(int version) {
        return get() >= version;
    }
}
