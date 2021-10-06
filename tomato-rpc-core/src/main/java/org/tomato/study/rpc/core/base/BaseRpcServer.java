package org.tomato.study.rpc.core.base;

import lombok.Getter;
import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.data.RpcServerConfig;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
public abstract class BaseRpcServer extends BaseLifeCycleComponent implements RpcServer {

    /**
     * 服务的ip
     */
    private final String host;

    /**
     * 服务的端口
     */
    private final int port;

    /**
     * 是否使用业务线程池
     */
    @Getter
    private final boolean useBusinessPool;

    /**
     * 若使用业务线程池，业务线程池核心线程数
     */
    @Getter
    private final int businessPoolSize;

    /**
     * 空闲连接检测时间
     */
    @Getter
    private final long readIdleCheckMilliseconds;

    public BaseRpcServer(RpcServerConfig rpcServerConfig) {
        this.host = rpcServerConfig.getHost();
        this.port = rpcServerConfig.getPort();
        this.useBusinessPool = rpcServerConfig.isUseBusinessThreadPool();
        this.businessPoolSize = rpcServerConfig.getBusinessThreadPoolSize();
        this.readIdleCheckMilliseconds = rpcServerConfig.getServerReadIdleCheckMilliseconds();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }
}
