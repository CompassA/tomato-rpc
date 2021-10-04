package org.tomato.study.rpc.core.base;

import lombok.Getter;
import org.tomato.study.rpc.core.NameServerFactory;
import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;
import org.tomato.study.rpc.core.spi.SpiLoader;

import java.util.List;

/**
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
     * stub factory for creating client stub
     */
    @Getter
    private final StubFactory stubFactory;

    /**
     * rpc configuration
     */
    private final RpcConfig rpcConfig;

    public BaseRpcCoreService(RpcConfig rpcConfig) {
        if (rpcConfig == null) {
            throw new TomatoRpcRuntimeException(
                    TomatoRpcCoreErrorEnum.RPC_CONFIG_INITIALIZING_ERROR.create());
        }
        this.rpcConfig = rpcConfig;
        this.providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();
        this.stubFactory = SpiLoader.getLoader(StubFactory.class).load();
        this.nameServer = SpiLoader.getLoader(NameServerFactory.class).load()
                .createNameService(
                        NameServerConfig.builder()
                                .connString(rpcConfig.getNameServiceURI())
                                .build()
                );
    }

    @Override
    public String getServiceVIP() {
        return rpcConfig.getServiceVIP();
    }

    @Override
    public List<String> getSubscribedVIP() {
        return rpcConfig.getSubscribedVIP();
    }

    @Override
    public String getStage() {
        return rpcConfig.getStage();
    }

    @Override
    public String getVersion() {
        return rpcConfig.getVersion();
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
}
