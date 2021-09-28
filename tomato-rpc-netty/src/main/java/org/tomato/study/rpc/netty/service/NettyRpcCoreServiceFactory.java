package org.tomato.study.rpc.netty.service;

import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcCoreServiceFactory;
import org.tomato.study.rpc.core.data.RpcConfig;

/**
 * @author Tomato
 * Created on 2021.09.28
 */
public class NettyRpcCoreServiceFactory implements RpcCoreServiceFactory {

    @Override
    public RpcCoreService create(RpcConfig rpcConfig) {
        return new NettyRpcCoreService(rpcConfig);
    }
}
