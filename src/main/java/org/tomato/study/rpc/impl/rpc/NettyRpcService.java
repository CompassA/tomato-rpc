package org.tomato.study.rpc.impl.rpc;

import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.SenderFactory;
import org.tomato.study.rpc.core.SpiLoader;
import org.tomato.study.rpc.core.StubFactory;

import java.io.IOException;
import java.net.URI;

/**
 * @author Tomato
 * Created on 2021.04.17
 */
public class NettyRpcService implements RpcCoreService {

    private final StubFactory stubFactory = SpiLoader.load(StubFactory.class);

    private final SenderFactory senderFactory = SpiLoader.load(SenderFactory.class);

    @Override
    public RpcCoreService startRpcServer() throws Exception {
        //todo
        return null;
    }

    @Override
    public <T> URI registerProvider(String serviceVIP,
                                    T serviceInstance,
                                    Class<T> serviceInterface) {
        //todo
        return null;
    }

    @Override
    public <T> T createStub(String serviceVIP, Class<T> serviceInterface) {
        try {
            return stubFactory.createStub(
                    senderFactory.create(serviceVIP), serviceInterface);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
        //todo
    }
}
