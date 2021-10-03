package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RPC服务配置
 * @author Tomato
 * Created on 2021.10.03
 */
@Getter
@AllArgsConstructor
public class RpcServerConfig {

    /**
     * 服务ip
     */
    private final String host;

    /**
     * 服务端口
     */
    private final int port;

    /**
     * 是否启用业务线程池
     */
    private final boolean useBusinessThreadPool;

    /**
     * 业务线程池大小
     */
    private final int businessThreadPoolSize;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String host = "127.0.0.1";
        private int port = 9090;
        private boolean useBusinessThreadPool = false;
        private int businessThreadPoolSize = 0;

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder useBusinessThreadPool(boolean useBusinessThreadPool) {
            this.useBusinessThreadPool = useBusinessThreadPool;
            return this;
        }

        public Builder businessThreadPoolSize(int businessThreadPoolSize) {
            this.businessThreadPoolSize = businessThreadPoolSize;
            return this;
        }

        public RpcServerConfig build() {
            return new RpcServerConfig(
                    host,
                    port,
                    useBusinessThreadPool,
                    businessThreadPoolSize
            );
        }
    }
}
