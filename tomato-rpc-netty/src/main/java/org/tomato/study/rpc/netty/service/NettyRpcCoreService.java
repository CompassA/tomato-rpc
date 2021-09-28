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
import org.tomato.study.rpc.core.NameServerFactory;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.base.BaseRpcCoreService;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.error.NettyRpcErrorEnum;
import org.tomato.study.rpc.netty.server.NettyRpcServer;
import org.tomato.study.rpc.utils.NetworkUtil;

import java.net.URI;

/**
 * Functions:
 *   1.start rpc server
 *   2.create client stub
 * @author Tomato
 * Created on 2021.04.17
 */
@Slf4j
public class NettyRpcCoreService extends BaseRpcCoreService {

    /**
     * application provider object mapper
     */
    private final ProviderRegistry providerRegistry;

    /**
     * name service for service registry and discovery
     */
    private final NameService nameService;

    /**
     * stub factory for creating client stub
     */
    private final StubFactory stubFactory;

    /**
     * application exported rpc server
     */
    private final NettyRpcServer server;

    public NettyRpcCoreService(RpcConfig rpcConfig) {
        super(rpcConfig);
        providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();
        stubFactory = SpiLoader.getLoader(StubFactory.class).load();
        nameService = SpiLoader.getLoader(NameServerFactory.class).load()
                .createNameService(
                        NameServerConfig.builder()
                                .connString(rpcConfig.getNameServiceURI())
                                .build()
                );
        server = new NettyRpcServer(NetworkUtil.getLocalHost(), getPort());
    }

    @Override
    public <T> URI registerProvider(T serviceInstance, Class<T> serviceInterface) {
        if (serviceInstance == null || serviceInterface == null ||
                !serviceInterface.isInterface()) {
            throw new TomatoRpcRuntimeException(NettyRpcErrorEnum.CORE_SERVICE_REGISTER_PROVIDER_ERROR.create());
        }

        providerRegistry.register(
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
        T stub = stubFactory.createStub(
                new StubConfig<>(
                        nameService,
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
        nameService.init();
        log.info("netty rpc core service initialized");
    }

    @Override
    protected void doStart() throws TomatoRpcException {
        try {
            // start rpc server
            server.start();

            // connected to name service
            nameService.start();

            // export self
            export(NetworkUtil.createURI(getProtocol(), server.getHost(), server.getPort()));

            // subscribe others
            nameService.subscribe(getSubscribedVIP(), getStage());
        } catch (Exception e) {
            throw new TomatoRpcException(NettyRpcErrorEnum.CORE_SERVICE_START_ERROR.create(), e);
        }

        // add shutdown hook
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    try {
                        stop();
                    } catch (TomatoRpcException e) {
                        log.error(e.getMessage(), e);
                    }
                })
        );
        log.info("netty rpc core service started");
    }

    @Override
    protected void doStop() throws TomatoRpcException {
        server.stop();
        nameService.stop();
        log.info("netty rpc core service stopped");
    }

    private void export(URI rpcServerURI) throws Exception {
        nameService.registerService(
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
