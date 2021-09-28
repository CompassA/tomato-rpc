package org.tomato.study.rpc.core.base;

import org.tomato.study.rpc.core.RpcCoreService;
import org.tomato.study.rpc.core.data.RpcConfig;
import org.tomato.study.rpc.core.error.TomatoRpcCoreErrorEnum;
import org.tomato.study.rpc.core.error.TomatoRpcRuntimeException;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;

import java.util.List;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
public abstract class BaseRpcCoreService extends BaseLifeCycleComponent implements RpcCoreService {

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
}
