package org.tomato.study.rpc.core;

import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.spi.SpiInterface;

/**
 * 创建RPC核心对象，支持SPI
 * @author Tomato
 * Created on 2021.09.28
 */
@SpiInterface
public interface RpcCoreServiceFactory {

    /**
     * 创建rpc核心对象
     * @param rpcConfig rpc配置
     * @return rpc核心对象
     */
    RpcCoreService create(RpcConfig rpcConfig);
}
