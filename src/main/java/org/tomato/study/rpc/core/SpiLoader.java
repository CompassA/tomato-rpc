package org.tomato.study.rpc.core;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public final class SpiLoader {

    private SpiLoader() throws IllegalAccessException {
        throw new IllegalAccessException("illegal exception");
    }

    public static <T> T load(Class<T> interfaceClazz) {
        // todo
        return (T) new Object();
    }
}
