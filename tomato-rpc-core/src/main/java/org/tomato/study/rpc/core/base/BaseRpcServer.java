package org.tomato.study.rpc.core.base;

import org.tomato.study.rpc.core.RpcServer;
import org.tomato.study.rpc.core.observer.BaseLifeCycleComponent;

/**
 * @author Tomato
 * Created on 2021.09.27
 */
public abstract class BaseRpcServer extends BaseLifeCycleComponent implements RpcServer {

    /**
     * application host
     */
    private final String host;

    /**
     * exported port
     */
    private final int port;

    public BaseRpcServer(String host, int port) {
        this.host = host;
        this.port = port;
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
