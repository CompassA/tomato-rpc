package org.tomato.study.rpc.core.spi;

/**
 * hold an volatile object
 * @author Tomato
 * Created on 2021.06.12
 */
public class ObjectHolder<T> {

    private volatile T object;

    public T get() {
        return object;
    }

    public void set(T object) {
        this.object = object;
    }
}
