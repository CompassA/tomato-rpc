/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.base;

import lombok.Getter;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.RpcServerConfig;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.invoker.RpcInvokerFactory;
import org.tomato.study.rpc.core.loadbalance.LoadBalance;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;
import org.tomato.study.rpc.core.registry.NameServer;
import org.tomato.study.rpc.core.registry.NameServerFactory;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.core.server.RpcServer;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.core.stub.StubFactory;
import org.tomato.study.rpc.utils.Logger;
import org.tomato.study.rpc.utils.NetworkUtil;

import java.util.List;

/**
 * RPC服务入口基类
 * Functions:
 * 1.根据设置的micro-service-id，将自己作为一个RPC服务暴露给其他服务
 * 2.将自己作为一个RPC客户端节点，订阅其他RPC服务
 * @author Tomato
 * Created on 2021.09.27
 */
public abstract class BaseRpcCoreService extends BaseLifeCycleComponent implements RpcCoreService {

    /**
     * application provider object mapper
     */
    @Getter
    private final ProviderRegistry providerRegistry;

    /**
     * name service for service registry and discovery
     */
    @Getter
    private final NameServer nameServer;

    /**
     * Invoker创建者
     */
    private final RpcInvokerFactory invokerFactory;

    /**
     * stub factory for creating client stub
     */
    @Getter
    private final StubFactory stubFactory;

    /**
     * 订阅的微服务对象
     */
    @Getter
    private final MicroServiceSpace[] microServices;

    /**
     * RPC服务器，接收客户端请求
     */
    @Getter
    private final RpcServer rpcServer;

    /**
     * 均衡负载
     */
    @Getter
    private final LoadBalance loadBalance;

    /**
     * RPC 服务元数据
     */
    @Getter
    private final MetaData rpcServerMetaData;

    /**
     * rpc configuration
     */
    private final RpcConfig rpcConfig;

    /**
     * rpc是否已经完成了上下文初始化
     */
    private boolean ready = false;

    public BaseRpcCoreService(RpcConfig rpcConfig) {
        this.rpcConfig = rpcConfig;
        this.providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();
        this.invokerFactory = SpiLoader.getLoader(RpcInvokerFactory.class).load();
        this.stubFactory = SpiLoader.getLoader(StubFactory.class).load();
        this.loadBalance = SpiLoader.getLoader(LoadBalance.class).load();

        // 创建注册中心
        NameServerConfig nameServerConfig = NameServerConfig.builder()
                .connString(rpcConfig.getNameServiceURI())
                .build();
        this.nameServer = SpiLoader.getLoader(NameServerFactory.class).load()
                .createNameService(nameServerConfig);

        // 创建rpc服务器
        RpcServerConfig rpcServerConfig = RpcServerConfig.builder()
                .host(NetworkUtil.getLocalHost())
                .port(rpcConfig.getPort())
                .useBusinessThreadPool(rpcConfig.getBusinessThreadPoolSize() > 1)
                .businessThreadPoolSize(rpcConfig.getBusinessThreadPoolSize())
                .clientKeepAliveMilliseconds(rpcConfig.getClientKeepAliveMilliseconds())
                .serverReadIdleCheckMilliseconds(rpcConfig.getServerIdleCheckMilliseconds())
                .build();
        this.rpcServer = createRpcServer(rpcServerConfig);
        String stage = getStage();
        MetaData.NodeProperty nodeProperty = new MetaData.NodeProperty();
        nodeProperty.weight = 1;
        this.rpcServerMetaData = MetaData.builder()
                .protocol(rpcConfig.getProtocol())
                .host(rpcServer.getHost())
                .port(rpcServer.getPort())
                .microServiceId(rpcConfig.getMicroServiceId())
                .stage(stage)
                .group(rpcConfig.getGroup())
                .nodeProperty(nodeProperty)
                .build();

        // 创建微服务对象
        this.microServices = createMicroServiceSpace(rpcConfig);
    }

    @Override
    public void updateServerProperty(MetaData.NodeProperty property) throws Exception {
        rpcServerMetaData.setNodeProperty(property);
        nameServer.registerService(rpcServerMetaData);
    }

    @Override
    public String getMicroServiceId() {
        return rpcConfig.getMicroServiceId();
    }

    @Override
    public List<String> getSubscribedServices() {
        return rpcConfig.getSubscribedServiceIds();
    }

    @Override
    public String getStage() {
        // jvm配置的环境优先级最高
        String stage = System.getProperty(RpcJvmConfigKey.MICRO_SERVICE_STAGE);
        if (stage != null) {
            return stage;
        }
        return rpcConfig.getStage();
    }

    @Override
    public String getGroup() {
        // jvm配置的group优先级最高
        String group = System.getProperty(RpcJvmConfigKey.MICRO_SERVICE_GROUP);
        if (group != null) {
            return group;
        }
        return rpcConfig.getGroup();
    }

    @Override
    public int getPort() {
        return rpcConfig.getPort();
    }

    @Override
    public String getProtocol() {
        return rpcConfig.getProtocol();
    }

    public RpcConfig getRpcConfig() {
        return rpcConfig;
    }

    @Override
    public RpcInvokerFactory getRpcInvokerFactory() {
        return invokerFactory;
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    protected void doInit() throws TomatoRpcException {
        // 初始化本地服务
        rpcServer.init();

        // 初始化注册中心
        nameServer.init();

        Logger.DEFAULT.info("netty rpc core service initialized");
    }

    @Override
    protected void doStart() throws TomatoRpcException {
        try {
            // 启动RPC服务器
            rpcServer.start();

            // 与注册中心建立连接
            nameServer.start();

            // 将自己的元数据上报至注册中心
            nameServer.registerService(rpcServerMetaData);

            // 订阅其余RPC服务
            nameServer.subscribe(microServices, getStage());
        } catch (Exception e) {
            throw new TomatoRpcException(TomatoRpcCoreErrorEnum.RPC_CONFIG_INITIALIZING_ERROR.create(), e);
        }
        ready = true;
        Logger.DEFAULT.info("netty rpc core service started. micro-service-id={},stage={},group={},host={},port={}",
                rpcConfig.getMicroServiceId(),
                rpcConfig.getStage(),
                rpcConfig.getGroup(),
                rpcServerMetaData.getHost(),
                rpcConfig.getPort());
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        String stage = getStage();
        try {
            nameServer.unsubscribe(microServices, stage);
            nameServer.unregisterService(rpcServerMetaData);
            for (MicroServiceSpace microService : microServices) {
                microService.close();
            }
        } catch (Exception e) {
            Logger.DEFAULT.error("name server unregister service failed", e);
        }
        rpcServer.stop();
        nameServer.stop();
        Logger.DEFAULT.info("netty rpc core service stopped");
    }

    /**
     * 初始化微服务对象
     * @param rpcConfig 微服务配置
     * @return 微服务对象
     */
    protected abstract MicroServiceSpace[] createMicroServiceSpace(RpcConfig rpcConfig);

    /**
     * 初始化rpc服务器
     * @param rpcServerConfig rpc服务器配置
     * @return rpc服务器
     */
    protected abstract RpcServer createRpcServer(RpcServerConfig rpcServerConfig);
}
