package net.tiny.service;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ClassHelper {


    //////////////////////////////////////////////////////////////////////////////////////
    /**
     * This helper method returns `true` only if the given
     * class type is an abstract class.
     *
     * @param type the class type to check
     * @return `true` if the given type is an abstract class, otherwise `false`
     */
    public static boolean isAbstractClass(Class<?> type) {
        return !type.isInterface() && Modifier.isAbstract(type.getModifiers());
    }

    public static boolean hasMethods(Class<?> classType) {
        Method[] methods = classType.getDeclaredMethods();
        return (null != methods && methods.length > 0);
    }

    public static List<Class<?>> getInterfaces(Class<?> classType) {
        return getInterfaces(classType, false, false, false);
    }
    public static List<Class<?>> getInterfaces(Class<?> classType,
            boolean inner,
            boolean constants,
            boolean serializable) {
        List<Class<?>> list = new ArrayList<Class<?>>();
        setInterfaces(classType, list, inner, constants, serializable);
        List<Class<?>> superClasses = getSuperClasses(classType);
        for(Class<?> c: superClasses) {
            setInterfaces(c, list, inner, constants, serializable);
        }
        return list;
    }

    private static void setInterfaces(Class<?> classType, List<Class<?>> list,
            boolean inner,
            boolean constants,
            boolean serializable) {
        Class<?>[] classes = classType.getInterfaces();
        for(Class<?> c: classes) {
            if (!list.contains(c)) {
                //TODO inner constants
                //Skip Serializable class
                if (!(!serializable && Serializable.class.equals(c))) {
                    list.add(c);
                }
            }
        }
    }

    public static List<Class<?>> getSuperClasses(Class<?> targetClass) {
        return getSuperClasses(targetClass, false);
    }

    public static List<Class<?>> getSuperClasses(Class<?> targetClass, boolean object) {
        List<Class<?>> list = getSuperClasses(targetClass, new ArrayList<Class<?>>());
        if (!object && list.size() > 0) {
            //Remove java.lang.Object
            list.remove(list.size()-1);
        }
        return list;
    }

    private static List<Class<?>> getSuperClasses(Class<?> targetClass, List<Class<?>> list) {
        Class<?> superClass = targetClass.getSuperclass();
        if(null != superClass) {
            list.add(superClass);
            list = getSuperClasses(superClass, list);
        }
        return list;
    }

    public static boolean isInnerClass(Class<?> classType) {
        return classType.getName().contains("$");
    }

    public static List<Field> findAnnotatedFields(Collection<Class<?>> classTypes, Class<? extends Annotation> annotation) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> type : classTypes) {
        	fields.addAll(findAnnotatedFields(type, annotation));
        }
        return fields;
    }

    public static List<Field> findAnnotatedFields(Class<?> type, Class<? extends Annotation> annotation) {
        List<Field> fields = new ArrayList<>();
        try {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(annotation)) {
                    fields.add(field);
                }
            }
        } catch (Throwable err) {
            // Ignore
        }
        return fields;
    }
}
