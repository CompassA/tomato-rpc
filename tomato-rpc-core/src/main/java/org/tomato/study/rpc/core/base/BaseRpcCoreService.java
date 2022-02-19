package org.tomato.study.rpc.core.base;

import lombok.Getter;
import org.tomato.study.rpc.core.NameServer;
import org.tomato.study.rpc.core.NameServerFactory;
import org.tomato.study.rpc.core.ProviderRegistry;
import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.RpcJvmConfigKey;
import org.tomato.study.rpc.core.StubFactory;
import org.tomato.study.rpc.core.data.NameServerConfig;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;
import org.tomato.study.rpc.core.spi.SpiLoader;
import org.tomato.study.rpc.core.transport.RpcInvokerFactory;

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
     * Invoker创建者
     */
    private final RpcInvokerFactory invokerFactory;

    /**
     * rpc configuration
     */
    private final RpcConfig rpcConfig;

    protected boolean ready = false;

    public BaseRpcCoreService(RpcConfig rpcConfig) {
        if (rpcConfig == null) {
            throw new TomatoRpcRuntimeException(
                    TomatoRpcCoreErrorEnum.RPC_CONFIG_INITIALIZING_ERROR.create());
        }
        this.rpcConfig = rpcConfig;
        this.providerRegistry = SpiLoader.getLoader(ProviderRegistry.class).load();
        this.stubFactory = SpiLoader.getLoader(StubFactory.class).load();
        this.invokerFactory = SpiLoader.getLoader(RpcInvokerFactory.class).load();
        NameServerConfig nameServerConfig = NameServerConfig.builder()
                .connString(rpcConfig.getNameServiceURI())
                .build();
        this.nameServer = SpiLoader.getLoader(NameServerFactory.class).load()
                .createNameService(nameServerConfig);
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
}
