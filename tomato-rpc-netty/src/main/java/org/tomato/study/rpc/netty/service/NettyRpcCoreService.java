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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.base.BaseRpcCoreService;
import org.tomato.study.rpc.core.data.ApiConfig;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.RpcServerConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.router.NettyMicroServiceSpace;
import org.tomato.study.rpc.netty.transport.server.NettyRpcServer;
import org.tomato.study.rpc.utils.NetworkUtil;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * 基于Netty实现的RPC服务入口类
 * Functions:
 * 1.根据设置的micro-service-id，将自己作为一个RPC服务暴露给其他服务
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
     * RPC 服务元数据
     */
    private final MetaData rpcServerMetaData;

    /**
     * 订阅的微服务对象
     */
    private final MicroServiceSpace[] microServices;

    public NettyRpcCoreService(RpcConfig rpcConfig) {
        super(rpcConfig);
        RpcServerConfig rpcServerConfig = RpcServerConfig.builder()
                .host(NetworkUtil.getLocalHost())
                .port(getPort())
                .useBusinessThreadPool(rpcConfig.getBusinessThreadPoolSize() > 1)
                .businessThreadPoolSize(rpcConfig.getBusinessThreadPoolSize())
                .clientKeepAliveMilliseconds(rpcConfig.getClientKeepAliveMilliseconds())
                .serverReadIdleCheckMilliseconds(rpcConfig.getServerIdleCheckMilliseconds())
                .build();
        this.server = new NettyRpcServer(rpcServerConfig);
        this.rpcServerMetaData = MetaData.builder()
                .protocol(getProtocol())
                .host(server.getHost())
                .port(server.getPort())
                .microServiceId(getMicroServiceId())
                .stage(getStage())
                .group(getGroup())
                .build();
        List<String> subscribedServiceIds = getSubscribedServices();
        if (CollectionUtils.isNotEmpty(subscribedServiceIds)) {
            this.microServices = new MicroServiceSpace[subscribedServiceIds.size()];
            for (int i = 0; i < subscribedServiceIds.size(); i++) {
                this.microServices[i] = new NettyMicroServiceSpace(
                        subscribedServiceIds.get(i),
                        getRpcInvokerFactory(),
                        rpcServerConfig.getClientKeepAliveMilliseconds(),
                        // todo 服务级别超时、接口级别超时
                        rpcConfig.getGlobalClientTimeoutMilliseconds());
            }
        } else {
            this.microServices = new MicroServiceSpace[0];
        }
    }

    @Override
    public <T> URI registerProvider(T serviceInstance, Class<T> serviceInterface) {
        if (serviceInstance == null || serviceInterface == null ||
                !serviceInterface.isInterface()) {
            throw new TomatoRpcRuntimeException(NettyRpcErrorEnum.CORE_SERVICE_REGISTER_PROVIDER_ERROR.create());
        }

        getProviderRegistry().register(
                getMicroServiceId(),
                serviceInstance,
                serviceInterface);
        URI providerURI = NetworkUtil.createURI(
                getProtocol(),
                server.getHost(),
                server.getPort());
        log.info("provider registered, URI[" + providerURI + "]");
        return providerURI;
    }

    @Override
    public <T> T createStub(ApiConfig<T> apiConfig) {
        Class<T> serviceInterface = apiConfig.getApi();
        if (!serviceInterface.isInterface()) {
            throw new TomatoRpcRuntimeException(NettyRpcErrorEnum.CORE_SERVICE_STUB_CREATE_ERROR.create());
        }
        T stub = getStubFactory().createStub(
                new StubConfig<>(
                        getNameServer(),
                        serviceInterface,
                        apiConfig.getMicroServiceId(),
                        // todo 默认调用同group，实现根据参数指定group
                        getGroup()
                ));
        log.info("stub " + serviceInterface.getCanonicalName() + " created");
        return stub;
    }

    @Override
    public <T> T createDirectStub(ApiConfig<T> apiConfig) {
        if (apiConfig == null) {
            return null;
        }
        MetaData nodeInfo = apiConfig.getNodeInfo();
        Class<T> api = apiConfig.getApi();
        if (nodeInfo == null || !nodeInfo.isValid() || !api.isInterface()) {
            return null;
        }
        RpcConfig rpcConfig = getRpcConfig();
        Long timeout = Optional.ofNullable(apiConfig.getTimeoutMs())
                .orElse(rpcConfig.getGlobalClientTimeoutMilliseconds());
        return getRpcInvokerFactory()
                .create(nodeInfo, rpcConfig.getClientKeepAliveMilliseconds(), timeout)
                .map(invoker -> getStubFactory().createStub(nodeInfo.getMicroServiceId(), invoker, api))
                .orElse(null);
    }

    @Override
    protected void doInit() throws TomatoRpcException {
        // 初始化本地服务
        server.init();

        // 初始化注册中心
        getNameServer().init();

        log.info("netty rpc core service initialized");
    }

    @Override
    protected void doStart() throws TomatoRpcException {
        NameServer nameServer = getNameServer();
        try {
            // 启动RPC服务器
            server.start();

            // 与注册中心建立连接
            nameServer.start();

            // 将自己的元数据上报至注册中心
            nameServer.registerService(rpcServerMetaData);

            // 订阅其余RPC服务
            nameServer.subscribe(microServices, getStage());
        } catch (Exception e) {
            throw new TomatoRpcException(NettyRpcErrorEnum.LIFE_CYCLE_START_ERROR.create(), e);
        }
        RpcConfig rpcConfig = getRpcConfig();
        log.info("netty rpc core service started.\n" +
                        "micro-service-id: {}\n" +
                        "stage: {}\n" +
                        "group: {}\n" +
                        "host: {}\n" +
                        "port: {}",
                rpcConfig.getMicroServiceId(),
                rpcConfig.getStage(),
                rpcConfig.getGroup(),
                rpcServerMetaData.getHost(),
                rpcConfig.getPort());
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        NameServer nameServer = getNameServer();
        try {
            nameServer.unsubscribe(microServices, getStage());
            nameServer.unregisterService(rpcServerMetaData);
            for (MicroServiceSpace microService : microServices) {
                microService.close();
            }
        } catch (Exception e) {
            log.error("name server unregister service failed", e);
        }
        server.stop();
        nameServer.stop();
        log.info("netty rpc core service stopped");
    }
}
