package org.tomato.study.rpc.core;

/**
 * create rpc server
 * @author Tomato
 * Created on 2021.06.12
 */
public interface RpcServerFactory {

    /**
     * create rpc server
     * @param host host
     * @param port port
     * @return rpc server
     */
    RpcServer create(String host, int port);
}
