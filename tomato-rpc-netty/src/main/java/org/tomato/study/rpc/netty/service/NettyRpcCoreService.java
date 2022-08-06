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
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.loadbalance.LoadBalance;
import org.tomato.study.rpc.core.server.RpcServer;
import org.tomato.study.rpc.core.base.BaseRpcCoreService;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.RpcServerConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.router.MicroServiceSpace;
import org.tomato.study.rpc.core.invoker.RpcInvokerFactory;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.router.NettyMicroServiceSpace;
import org.tomato.study.rpc.netty.transport.server.NettyRpcServer;
import org.tomato.study.rpc.utils.NetworkUtil;

import java.net.URI;
import java.util.List;

/**
 * 基于Netty实现的RPC服务入口类
 * @author Tomato
 * Created on 2021.04.17
 */
@Slf4j
public class NettyRpcCoreService extends BaseRpcCoreService {

    public NettyRpcCoreService(RpcConfig rpcConfig) {
        super(rpcConfig);
    }

    @Override
    public <T> URI registerProvider(T serviceInstance, Class<T> serviceInterface) {
        if (serviceInstance == null || serviceInterface == null ||
                !serviceInterface.isInterface()) {
            throw new TomatoRpcRuntimeException(NettyRpcErrorEnum.CORE_SERVICE_REGISTER_PROVIDER_ERROR.create());
        }
        ProviderRegistry providerRegistry = getProviderRegistry();
        String microServiceId = getMicroServiceId();
        providerRegistry.register(microServiceId, serviceInstance, serviceInterface);

        RpcServer rpcServer = getRpcServer();
        URI providerURI = NetworkUtil.createURI(getProtocol(), rpcServer.getHost(), rpcServer.getPort());
        log.info("provider registered, URI[" + providerURI + "]");
        return providerURI;
    }

    @Override
    public <T> T createStub(StubConfig<T> stubConfig) {
        T stub = getStubFactory().createStub(getRpcConfig(), stubConfig, getRpcInvokerFactory());
        log.info("stub " + stubConfig.getServiceInterface().getCanonicalName() + " created");
        return stub;
    }

    @Override
    protected MicroServiceSpace[] createMicroServiceSpace(RpcConfig rpcConfig) {
        List<String> subscribedServiceIds = rpcConfig.getSubscribedServiceIds();
        MicroServiceSpace[] microServices;
        if (CollectionUtils.isNotEmpty(subscribedServiceIds)) {
            microServices = new MicroServiceSpace[subscribedServiceIds.size()];
            RpcInvokerFactory rpcInvokerFactory = getRpcInvokerFactory();
            LoadBalance loadBalance = getLoadBalance();
            for (int i = 0; i < subscribedServiceIds.size(); i++) {
                microServices[i] = new NettyMicroServiceSpace(
                        subscribedServiceIds.get(i), rpcInvokerFactory, rpcConfig, loadBalance);
            }
        } else {
            microServices = new MicroServiceSpace[0];
        }
        return microServices;
    }

    @Override
    protected RpcServer createRpcServer(RpcServerConfig rpcServerConfig) {
        return new NettyRpcServer(rpcServerConfig);
    }
}
