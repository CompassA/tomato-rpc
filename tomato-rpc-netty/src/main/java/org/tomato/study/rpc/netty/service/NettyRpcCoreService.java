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

import lombok.Getter;
import org.tomato.study.rpc.core.MessageSender;
import org.tomato.study.rpc.core.NameService;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.RpcServerFactory;
import org.tomato.study.rpc.core.SenderFactory;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.utils.NetworkUtil;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public class NettyRpcCoreService implements RpcCoreService {

    private static final String PROTOCOL = "tomato";

    private final String serviceVIP;

    @Getter
    private final List<String> subscribedVIP;

    @Getter
    private final URI nameServerURI;

    private final StubFactory stubFactory = SpiLoader.getLoader(StubFactory.class).load();

    private final SenderFactory senderFactory = SpiLoader.getLoader(SenderFactory.class).load();

    private final ProviderRegistry providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();

    private final RpcServerFactory rpcServerFactory = SpiLoader.getLoader(RpcServerFactory.class).load();

    private NameService nameService = SpiLoader.getLoader(NameService.class).load();

    private final Map<String, MessageSender> senderMap = new ConcurrentHashMap<>();

    private RpcServer server = null;

    public NettyRpcCoreService(String serviceVIP, List<String> subscribedVIP, URI nameServerURI) {
        this.serviceVIP = serviceVIP;
        this.subscribedVIP = subscribedVIP;
        this.nameServerURI = nameServerURI;
        this.nameService.connect(this.nameServerURI, this.subscribedVIP);
    }

    @Override
    public synchronized RpcServer startRpcServer(int port) throws Exception {
        if (this.server != null) {
            throw new IllegalStateException("multi server");
        }
        URI uri = startServer(port);
        export(uri);
        return this.server;
    }

    private URI startServer(int port) {
        String localHost = NetworkUtil.getLocalHost();
        this.server = this.rpcServerFactory.create(localHost, port);
        this.server.start();
        return NetworkUtil.createURI(PROTOCOL, localHost, port);
    }

    private void export(URI rpcServerURI) {
        MetaData metadata = MetaData.builder()
                .uri(rpcServerURI)
                .vip(this.serviceVIP)
                .build();
        this.nameService.registerService(metadata);
    }

    @Override
    public <T> URI registerProvider(T serviceInstance, Class<T> serviceInterface) {
        providerRegistry.register(serviceVIP, serviceInstance, serviceInterface);
        return NetworkUtil.createURI(PROTOCOL, server.getHost(), server.getPort());
    }

    @Override
    public <T> T createStub(String serviceVIP, Class<T> serviceInterface) {
        try {
            MessageSender sender = senderMap.computeIfAbsent(serviceVIP, senderFactory::create);
            return stubFactory.createStub(sender, serviceInterface, serviceVIP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getServiceVIP() {
        return this.serviceVIP;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.server != null) {
            this.server.close();
            this.server = null;
        }
        if (this.nameService != null) {
            this.nameService.disconnect();
            this.nameService = null;
        }
        for (MessageSender sender : senderMap.values()) {
            sender.close();
        }
        senderMap.clear();
    }
}
