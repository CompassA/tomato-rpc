package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * @author Tomato
 * Created on 2021.04.18
 */
@SpiInterface(paramName = "providerRegistry")
public interface ProviderRegistry {

    /**
     * register provider handler
     * @param vip virtual ip
     * @param instance provider instance
     * @param providerInterface service provider interface
     * @param <T> provider interface type
     */
    <T> void register(String vip, T instance, Class<T> providerInterface);

    /**
     * get provider instance
     * @param vip virtual ip
     * @param providerInterface service provider interface
     * @return provider instance
     */
    Object getProvider(String vip, Class<?> providerInterface);
}
