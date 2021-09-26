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

import org.tomato.study.rpc.core.NameServerFactory;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.base.BaseNameService;
import org.tomato.study.rpc.core.base.BaseRpcCoreService;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.server.NettyRpcServer;
import org.tomato.study.rpc.utils.NetworkUtil;

import java.net.URI;
import java.util.Collection;

/**
 * Functions:
 *   1.start rpc server
 *   2.create client stub
 * @author Tomato
 * Created on 2021.04.17
 */
public class NettyRpcCoreService extends BaseRpcCoreService {

    /**
     * application provider object mapper
     */
    private final ProviderRegistry providerRegistry;

    /**
     * name service for service registry and discovery
     */
    private final BaseNameService nameService;

    /**
     * application exported rpc server
     */
    private final NettyRpcServer server;

    public NettyRpcCoreService(RpcConfig rpcConfig) {
        super(rpcConfig);
        providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();
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
        this.providerRegistry.register(
                getServiceVIP(),
                serviceInstance,
                serviceInterface
        );
        return NetworkUtil.createURI(
                getProtocol(),
                server.getHost(),
                server.getPort()
        );
    }

    @Override
    public <T> T createStub(String serviceVIP, Class<T> serviceInterface) {
        StubConfig<T> stubConfig = new StubConfig<>(
                nameService,
                serviceInterface,
                serviceVIP,
                getVersion()
        );
        return SpiLoader.getLoader(StubFactory.class)
                .load()
                .createStub(stubConfig);
    }

    @Override
    public void subscribe(Collection<String> vipList) throws Exception {
        this.nameService.subscribe(vipList, getStage());
    }

    @Override
    protected void doInit() {
        server.init();
        nameService.init();
    }

    @Override
    protected void doStart() {
        server.start();

        nameService.start();
        export(NetworkUtil.createURI(getProtocol(), server.getHost(), server.getPort()));

        Runtime.getRuntime().addShutdownHook(
                new Thread(NettyRpcCoreService.this::stop)
        );
    }

    @Override
    protected void doStop() {
        server.stop();
        nameService.stop();
    }

    private void export(URI rpcServerURI) {
        this.nameService.registerService(
                MetaData.builder()
                        .protocol(rpcServerURI.getScheme())
                        .host(rpcServerURI.getHost())
                        .port(rpcServerURI.getPort())
                        .vip(getServiceVIP())
                        .stage(getStage())
                        .version(getVersion())
                        .build()
        );
    }
}
