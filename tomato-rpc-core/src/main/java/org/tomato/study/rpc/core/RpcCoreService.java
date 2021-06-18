package org.tomato.study.rpc.core;

import java.io.Closeable;
import java.net.URI;

/**
 * core method of rpc server
 * @author Tomato
 * Created on 2021.03.30
 */
public interface RpcCoreService extends Closeable {

    /**
     * create rpc server
     * @return rpc server instance
     * @param port bind port
     * @throws Exception start exception
     */
    RpcServer startRpcServer(int port) throws Exception;

    /**
     * register service provider
     * @param serviceVIP service virtual ip
     * @param serviceInstance service bean
     * @param serviceInterface service interface class
     * @param <T> service type
     * @return service address
     */
    <T> URI registerProvider(String serviceVIP,
                             T serviceInstance,
                             Class<T> serviceInterface);

    /**
     * create client proxy consumer
     * @param serviceVIP service virtual ip
     * @param serviceInterface consumer interface class
     * @param <T> consumer class type
     * @return proxy instance
     */
    <T> T createStub(String serviceVIP, Class<T> serviceInterface);
}