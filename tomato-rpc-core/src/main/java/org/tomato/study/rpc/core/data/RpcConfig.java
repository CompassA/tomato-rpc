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
    private final String microServiceId;

    /**
     * 当前服务所在的环境，用于环境隔离
     */
    private final String stage;

    /**
     * 当前服务的分组
     */
    private final String group;

    /**
     * 当前服务订阅的其他RPC服务
     */
    private final List<String> subscribedServiceIds;

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

    /**
     * 服务端空闲检测频率, 单位ms
     */
    private final long serverIdleCheckMilliseconds;

    /**
     * 客户端心跳发送频率, 单位ms
     */
    private final long clientKeepAliveMilliseconds;

    /**
     * 全局RPC客户端超时时间
     */
    private final long globalClientTimeoutMilliseconds;

    /**
     * 是否开启熔断
     */
    private final boolean enableCircuit;

    /**
     * 错误率超过多少时开启熔断
     */
    private final double circuitOpenRate;

    /**
     * 断路器开启多久后进入半开模式
     */
    private final long circuitOpenSeconds;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String protocol = "tomato";
        private String microServiceId;
        private String stage = "default";
        private String group = "default";
        private List<String> subscribedServiceIds = Collections.emptyList();
        private String nameServiceURI;
        private int port = 9090;
        private int businessThreadPoolSize = 1;
        private boolean useGzip = false;
        private long serverIdleCheckMilliseconds = 600000;
        private long clientKeepAliveMilliseconds = serverIdleCheckMilliseconds / 3;
        private long globalClientTimeoutMilliseconds = 50000;
        private boolean enableCircuit = false;
        private double circuitOpenRate = 0.75;
        private long circuitOpenSeconds = 60;

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder microServiceId(String microServiceId) {
            this.microServiceId = microServiceId;
            return this;
        }

        public Builder stage(String stage) {
            this.stage = stage;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder subscribedServiceIds(List<String> subscribedServiceIds) {
            this.subscribedServiceIds = subscribedServiceIds;
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

        public Builder serverIdleCheckMilliseconds(long serverIdleCheckMilliseconds) {
            this.serverIdleCheckMilliseconds = serverIdleCheckMilliseconds;
            return this;
        }

        public Builder clientKeepAliveMilliseconds(long clientKeepAliveMilliseconds) {
            this.clientKeepAliveMilliseconds = clientKeepAliveMilliseconds;
            return this;
        }

        public Builder globalClientTimeoutMilliseconds(long globalClientTimeoutMilliseconds) {
            this.globalClientTimeoutMilliseconds = globalClientTimeoutMilliseconds;
            return this;
        }

        public Builder enableCircuit(boolean enableCircuit) {
            this.enableCircuit = enableCircuit;
            return this;
        }

        public Builder circuitOpenRate(double circuitOpenRate) {
            this.circuitOpenRate = circuitOpenRate;
            return this;
        }

        public Builder circuitOpenSeconds(long circuitOpenSeconds) {
            this.circuitOpenSeconds = circuitOpenSeconds;
            return this;
        }

        public RpcConfig build() {
            return new RpcConfig(
                    this.protocol,
                    this.microServiceId,
                    this.stage,
                    this.group,
                    this.subscribedServiceIds,
                    this.nameServiceURI,
                    this.port,
                    this.businessThreadPoolSize,
                    this.useGzip,
                    this.serverIdleCheckMilliseconds,
                    this.clientKeepAliveMilliseconds,
                    this.globalClientTimeoutMilliseconds,
                    this.enableCircuit,
                    this.circuitOpenRate,
                    this.circuitOpenSeconds
            );
        }
    }
}
