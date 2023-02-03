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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.tomato.study.rpc.core.registry.NameServer;

/**
 * necessary data for creating stub
 * @author Tomato
 * Created on 2021.07.13
 */
@Getter
@ToString
public class StubConfig<T> {

    /**
     * interface of service provider
     */
    private final Class<T> serviceInterface;

    /**
     * service id
     */
    private final String microServiceId;

    /**
     * 分组
     */
    private final String group;

    /**
     * 是否压缩
     */
    private final boolean compressBody;

    /**
     * 调用超时等待时间
     */
    private final Long timeoutMs;

    /**
     * rpc-service节点数据，非必传，客户端直连时传这个参数
     */
    @Setter
    private MetaData nodeInfo;

    /**
     * 基于服务发现的rpc需要NameServer
     */
    @Setter
    private NameServer nameServer;

    /**
     * 基于服务发现的Stub
     */
    public StubConfig(
            Class<T> serviceInterface,
            String microServiceId,
            String group,
            boolean compressBody,
            Long timeoutMs,
            NameServer nameServer) {
        this.serviceInterface = serviceInterface;
        this.microServiceId = microServiceId;
        this.group = group;
        this.compressBody = compressBody;
        this.timeoutMs = timeoutMs;
        this.nameServer = nameServer;
    }

    /**
     * 基于ip直连的stub
     */
    public StubConfig(
            Class<T> serviceInterface,
            String microServiceId,
            String group,
            boolean compressBody,
            Long timeoutMs,
            MetaData nodeInfo) {
        this.serviceInterface = serviceInterface;
        this.microServiceId = microServiceId;
        this.group = group;
        this.compressBody = compressBody;
        this.timeoutMs = timeoutMs;
        this.nodeInfo = nodeInfo;
    }

    public boolean isValid() {
        return serviceInterface != null
                && StringUtils.isNotBlank(microServiceId)
                && StringUtils.isNotBlank(group)
                && timeoutMs != null
                && serviceInterface.isInterface()
                // RPC直连 或者 服务发现，二选一
                && (nodeInfo != null || nameServer != null);
    }
}
