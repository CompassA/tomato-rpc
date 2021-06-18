package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * create rpc server
 * @author Tomato
 * Created on 2021.06.12
 */
@SpiInterface(paramName = "rpcServerFactory")
public interface RpcServerFactory {

    /**
     * create rpc server
     * @param host host
     * @param port port
     * @return rpc server
     */
    RpcServer create(String host, int port);
}
