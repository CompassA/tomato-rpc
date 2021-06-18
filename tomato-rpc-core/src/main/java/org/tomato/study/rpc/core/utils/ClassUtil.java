package org.tomato.study.rpc.core.utils;

/**
 * @author Tomato
 * Created on 2021.06.13
 */
public final class ClassUtil {

    private ClassUtil() throws IllegalAccessException {
        throw new IllegalAccessException();
    }

    /**
     * search the ClassLoader in the following order:
     * 1. context class loader
     * 2. class type loader
     * 3. system class loader
     * @param clazz target class type
     * @return class loader
     */
    public static ClassLoader getClassLoader(Class<?> clazz) {
        ClassLoader classLoader = null;
        try {
            classLoader = Thread.currentThread().getContextClassLoader();
        } catch (Throwable e) {
            // do nothing, continue
        }
        if (classLoader == null) {
            try {
                classLoader = clazz.getClassLoader();
            } catch (Throwable e) {
                // do nothing, continue
            }
        }
        if (classLoader == null) {
            try {
                classLoader = ClassLoader.getSystemClassLoader();
            } catch (Throwable e) {
                // do nothing
            }
        }
        return classLoader;
    }
}
