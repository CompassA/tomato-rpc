package org.tomato.study.rpc.netty.server;

import org.tomato.study.rpc.core.HandlerRegistry;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.RpcServerFactory;

/**
 * @author Tomato
 * Created on 2021.06.12
 */
public class NettyRpcServerFactory implements RpcServerFactory {

    @Override
    public RpcServer create(String host, int port) {
        return new NettyRpcServer(host, port, new HandlerRegistry());
    }
}
