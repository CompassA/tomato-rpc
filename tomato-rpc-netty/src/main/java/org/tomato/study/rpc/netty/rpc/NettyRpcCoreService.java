package org.tomato.study.rpc.netty.rpc;

import org.tomato.study.rpc.core.HandlerRegistry;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.SenderFactory;
import org.tomato.study.rpc.core.SpiLoader;
import org.tomato.study.rpc.core.StubFactory;

import java.io.IOException;
import java.net.URI;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public class NettyRpcCoreService implements RpcCoreService {

    private final StubFactory stubFactory = SpiLoader.load(StubFactory.class);

    private final SenderFactory senderFactory = SpiLoader.load(SenderFactory.class);

    private final ProviderRegistry providerRegistry = SpiLoader.load(ProviderRegistry.class);

    private RpcServer server = null;

    private final HandlerRegistry handlerRegistry = new HandlerRegistry();

    @Override
    public synchronized RpcServer startRpcServer(int port) throws Exception {
        if (server != null) {
            throw new IllegalStateException("multi server");
        }
        this.server = SpiLoader.load(RpcServer.class);
        server.start(handlerRegistry, port);
        return server;
    }

    @Override
    public <T> URI registerProvider(String serviceVIP,
                                    T serviceInstance,
                                    Class<T> serviceInterface) {
        providerRegistry.register(serviceVIP, serviceInstance, serviceInterface);
        //todo name service
        return URI.create("tomato://127.0.0.1:" + server.getPort());
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
