package org.tomato.study.rpc.core;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
public interface ProviderRegistry {

    /**
     * register provider handler
     * @param vip virtual ip
     * @param instance provider instance
     * @param serviceInterface service provider interface
     * @param <T> provider interface type
     */
    <T> void register(String vip, T instance, Class<T> serviceInterface);
}
