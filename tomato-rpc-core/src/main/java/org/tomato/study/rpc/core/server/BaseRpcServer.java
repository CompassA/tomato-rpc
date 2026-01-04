/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.tomato.study.rpc.core.server;

import lombok.Getter;
import org.tomato.study.rpc.core.ProviderRegistry;
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

    /**
     * 服务接口实现
     */
    @Getter
    private final ProviderRegistry providerRegistry;

    public BaseRpcServer(RpcServerConfig rpcServerConfig, ProviderRegistry providerRegistry) {
        this.host = rpcServerConfig.getHost();
        this.port = rpcServerConfig.getPort();
        this.useBusinessPool = rpcServerConfig.isUseBusinessThreadPool();
        this.businessPoolSize = rpcServerConfig.getBusinessThreadPoolSize();
        this.readIdleCheckMilliseconds = rpcServerConfig.getServerReadIdleCheckMilliseconds();
        this.providerRegistry = providerRegistry;
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
