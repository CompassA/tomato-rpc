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

package org.tomato.study.rpc.core.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * RPC配置
 * @author Tomato
 * Created on 2021.07.11
 */
@Getter
@AllArgsConstructor
public class RpcConfig {

    /**
     * RPC协议
     */
    private final String protocol;

    /**
     * 当前服务的唯一标识，别的服务通过该标识订阅服务
     */
    private final String serviceVIP;

    /**
     * 当前服务所在的环境，用于环境隔离
     */
    private final String stage;

    /**
     * 当前服务的接口版本，用于同环境间不同服务版本的隔离
     */
    private final String version;

    /**
     * 当前服务订阅的其他RPC服务
     */
    private final List<String> subscribedVIP;

    /**
     * 注册中心的连接URI
     */
    private final String nameServiceURI;

    /**
     * RPC服务暴露在哪个端口
     */
    private final int port;

    /**
     * 业务线程池配置
     */
    private final int businessThreadPoolSize;

    /**
     * rpc body体是否使用Gzip压缩
     */
    private final boolean useGzip;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String protocol = "tomato";
        private String serviceVIP;
        private String stage = "default";
        private String version = "default";
        private List<String> subscribedVIP = Collections.emptyList();
        private String nameServiceURI;
        private int port = 9090;
        private int businessThreadPoolSize = 0;
        private boolean useGzip = false;

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder serviceVIP(String serviceVIP) {
            this.serviceVIP = serviceVIP;
            return this;
        }

        public Builder stage(String stage) {
            this.stage = stage;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder subscribedVIP(List<String> subscribedVIP) {
            this.subscribedVIP = subscribedVIP;
            return this;
        }

        public Builder nameServiceURI(String nameServiceURI) {
            this.nameServiceURI = nameServiceURI;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder businessThreadPoolSize(int businessThreadPoolSize) {
            this.businessThreadPoolSize = businessThreadPoolSize;
            return this;
        }

        public Builder useGzip(boolean useGzip) {
            this.useGzip = useGzip;
            return this;
        }

        public RpcConfig build() {
            return new RpcConfig(
                    this.protocol,
                    this.serviceVIP,
                    this.stage,
                    this.version,
                    this.subscribedVIP,
                    this.nameServiceURI,
                    this.port,
                    this.businessThreadPoolSize,
                    this.useGzip
            );
        }
    }
}
