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

import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.RpcServerFactory;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.data.StubConfig;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.utils.NetworkUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public class NettyRpcCoreService implements RpcCoreService {

    private static final String PROTOCOL = "tomato";

    private final String serviceVIP;

    private final List<String> subscribedVIP;

    private final String stage;

    private final String version;

    private final ProviderRegistry providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();

    private final NameService nameService = SpiLoader.getLoader(NameService.class).load();

    private RpcServer server = null;

    public NettyRpcCoreService(RpcConfig rpcConfig) {
        this.serviceVIP = rpcConfig.getServiceVIP();
        this.subscribedVIP = rpcConfig.getSubscribedVIP();
        this.stage = rpcConfig.getStage();
        this.version = rpcConfig.getVersion();
        this.nameService.connect(rpcConfig.getNameServiceURI());
    }

    @Override
    public synchronized RpcServer startRpcServer(int port) throws Exception {
        if (this.server != null) {
            throw new IllegalStateException("multi server");
        }
        URI uri = this.startServer(port);
        this.export(uri);
        return this.server;
    }

    private URI startServer(int port) {
        String localHost = NetworkUtil.getLocalHost();
        this.server = SpiLoader.getLoader(RpcServerFactory.class)
                .load()
                .create(localHost, port);
        this.server.start();
        return NetworkUtil.createURI(PROTOCOL, localHost, port);
    }

    private void export(URI rpcServerURI) {
        this.nameService.registerService(
                MetaData.builder()
                        .protocol(rpcServerURI.getScheme())
                        .host(rpcServerURI.getHost())
                        .port(rpcServerURI.getPort())
                        .vip(this.serviceVIP)
                        .stage(this.stage)
                        .version(this.version)
                        .build()
        );
    }

    @Override
    public <T> URI registerProvider(T serviceInstance, Class<T> serviceInterface) {
        this.providerRegistry.register(serviceVIP, serviceInstance, serviceInterface);
        return NetworkUtil.createURI(
                PROTOCOL,
                this.server.getHost(),
                this.server.getPort()
        );
    }

    @Override
    public <T> T createStub(String serviceVIP, Class<T> serviceInterface) {
        StubConfig<T> stubConfig = new StubConfig<>(
                this.nameService,
                serviceInterface,
                serviceVIP,
                this.version
        );
        return SpiLoader.getLoader(StubFactory.class)
                .load()
                .createStub(stubConfig);
    }

    @Override
    public void subscribe(Collection<String> vipList) throws Exception {
        this.nameService.subscribe(vipList, this.stage);
    }

    @Override
    public String getServiceVIP() {
        return this.serviceVIP;
    }

    @Override
    public List<String> getSubscribedVIP() {
        return this.subscribedVIP;
    }

    @Override
    public String getStage() {
        return this.stage;
    }

    @Override
    public String getVersion() {
        return this.version;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.server != null) {
            this.server.close();
            this.server = null;
        }
        if (this.nameService != null) {
            this.nameService.disconnect();
        }
    }
}
