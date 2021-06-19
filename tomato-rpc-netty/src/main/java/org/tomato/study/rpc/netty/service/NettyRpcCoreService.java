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

import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.RpcServerFactory;
import org.tomato.study.rpc.core.SenderFactory;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.netty.utils.NetworkUtil;

import java.io.IOException;
import java.net.URI;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public class NettyRpcCoreService implements RpcCoreService {

    private final StubFactory stubFactory = SpiLoader.getLoader(StubFactory.class).load();

    private final SenderFactory senderFactory = SpiLoader.getLoader(SenderFactory.class).load();

    private final ProviderRegistry providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();

    private final RpcServerFactory rpcServerFactory = SpiLoader.getLoader(RpcServerFactory.class).load();

    private RpcServer server = null;

    @Override
    public synchronized RpcServer startRpcServer(int port) throws Exception {
        if (server != null) {
            throw new IllegalStateException("multi server");
        }
        this.server = this.rpcServerFactory.create(NetworkUtil.getLocalHost(), port);
        server.start();
        return server;
    }

    @Override
    public <T> URI registerProvider(String serviceVIP,
                                    T serviceInstance,
                                    Class<T> serviceInterface) {
        providerRegistry.register(serviceVIP, serviceInstance, serviceInterface);
        return URI.create(String.format("tomato://%s:%d", server.getHost(), server.getPort()));
    }

    @Override
    public <T> T createStub(String serviceVIP, Class<T> serviceInterface) {
        try {
            return stubFactory.createStub(
                    senderFactory.create(serviceVIP), serviceInterface, serviceVIP);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        if (server != null) {
            server.close();
            server = null;
        }
    }
}
