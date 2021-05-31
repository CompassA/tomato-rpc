package org.tomato.study.rpc.core;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public final class SpiLoader {

    private SpiLoader() throws IllegalAccessException {
        throw new IllegalAccessException("illegal exception");
    }

    @SuppressWarnings("unchecked")
    public static <T> T load(Class<T> interfaceClazz) {
        // hardcode now; todo implement spi
        return (T) new Object();
    }
    
     public static <T> Collection<T> loadAll(Class<T> clazz) {
         // TODO: 2021/4/19  
        if (!clazz.isInterface()) {
            return Collections.emptyList();
        }
        return Collections.emptyList();
    } 
}
