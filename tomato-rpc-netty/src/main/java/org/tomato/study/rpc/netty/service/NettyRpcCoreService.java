/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.netty.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.base.BaseRpcCoreService;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.router.ServiceProviderFactory;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.handler.ResponseHandler;
import org.tomato.study.rpc.netty.router.NettyServiceProviderFactory;
import org.tomato.study.rpc.netty.client.NettyChannelHolder;
import org.tomato.study.rpc.netty.client.NettyResponseHolder;
import org.tomato.study.rpc.netty.server.NettyRpcServer;
import org.tomato.study.rpc.utils.NetworkUtil;

import java.net.URI;

/**
 * 基于Netty实现的RPC服务入口类
 * Functions:
 * 1.根据设置的VIP，将自己作为一个RPC服务暴露给其他服务
 * 2.将自己作为一个RPC客户端节点，订阅其他RPC服务
 * @author Tomato
 * Created on 2021.04.17
 */
@Slf4j
public class NettyRpcCoreService extends BaseRpcCoreService {

    /**
     * RPC服务器，接收客户端请求
     */
    private final NettyRpcServer server;

    /**
     * 管理与RPC服务端建立的所有连接
     */
    private final NettyChannelHolder channelHolder;

    /**
     * 管理所有响应Future
     */
    private final NettyResponseHolder responseHolder;

    public NettyRpcCoreService(RpcConfig rpcConfig) {
        super(rpcConfig);
        this.server = new NettyRpcServer(NetworkUtil.getLocalHost(), getPort());
        this.responseHolder = new NettyResponseHolder();
        this.channelHolder = new NettyChannelHolder(
                Lists.newArrayList(new ResponseHandler(this.responseHolder))
        );
    }

    @Override
    public <T> URI registerProvider(T serviceInstance, Class<T> serviceInterface) {
        if (serviceInstance == null || serviceInterface == null ||
                !serviceInterface.isInterface()) {
            throw new TomatoRpcRuntimeException(NettyRpcErrorEnum.CORE_SERVICE_REGISTER_PROVIDER_ERROR.create());
        }

        getProviderRegistry().register(
                getServiceVIP(),
                serviceInstance,
                serviceInterface
        );
        URI providerURI = NetworkUtil.createURI(
                getProtocol(),
                server.getHost(),
                server.getPort()
        );
        log.info("provider registered, URI[" + providerURI + "]");
        return providerURI;
    }

    @Override
    public <T> T createStub(String targetServiceVIP, Class<T> serviceInterface) {
        if (!serviceInterface.isInterface()) {
            throw new TomatoRpcRuntimeException(NettyRpcErrorEnum.CORE_SERVICE_STUB_CREATE_ERROR.create());
        }
        T stub = getStubFactory().createStub(
                new StubConfig<>(
                        getNameService(),
                        serviceInterface,
                        targetServiceVIP,
                        getVersion()
                )
        );
        log.info("stub " + serviceInterface.getCanonicalName() + " created");
        return stub;
    }

    @Override
    protected void doInit() throws TomatoRpcException {
        server.init();
        getNameService().init();
        SpiLoader.registerLoader(
                ServiceProviderFactory.class,
                new NettyServiceProviderFactory(channelHolder, responseHolder)
        );
        log.info("netty rpc core service initialized");
    }

    @Override
    protected void doStart() throws TomatoRpcException {
        NameService nameService = getNameService();
        try {
            // 启动RPC服务器
            server.start();

            // 与注册中心建立连接
            nameService.start();

            // 将自己的元数据上报至注册中心
            export(NetworkUtil.createURI(getProtocol(), server.getHost(), server.getPort()));

            // 订阅其余RPC服务
            nameService.subscribe(getSubscribedVIP(), getStage());

        } catch (Exception e) {
            throw new TomatoRpcException(NettyRpcErrorEnum.CORE_SERVICE_START_ERROR.create(), e);
        }
        log.info("netty rpc core service started");
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        server.stop();
        getNameService().stop();
        channelHolder.close();
        log.info("netty rpc core service stopped");
    }

    private void export(URI rpcServerURI) throws Exception {
        getNameService().registerService(
                MetaData.builder()
                        .protocol(rpcServerURI.getScheme())
                        .host(rpcServerURI.getHost())
                        .port(rpcServerURI.getPort())
                        .vip(getServiceVIP())
                        .stage(getStage())
                        .version(getVersion())
                        .build()
        );
        log.info("rpc node URI[" + rpcServerURI + "] exported");
    }
}
