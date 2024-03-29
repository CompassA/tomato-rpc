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

package org.tomato.study.rpc.core.router;

import org.tomato.study.rpc.core.data.Invocation;
import org.tomato.study.rpc.core.data.MetaData;
import org.tomato.study.rpc.core.error.TomatoRpcException;
import org.tomato.study.rpc.core.invoker.RpcInvoker;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 代表一个微服务对象
 * @author Tomato
 * Created on 2021.07.07
 */
public interface MicroServiceSpace {

    /**
     * 微服务唯一标识
     * ex. "trade-rpc-service"
     * @return id
     */
    String getMicroServiceId();

    /**
     * 获取所有的invoker
     * @return invokers
     */
    List<RpcInvoker> getAllInvokers();

    /**
     * 根据微服务分组找到匹配的一个微服务调用实例
     * @param group service group
     * @param invocation rpc request data
     * @return invoker
     */
    Optional<RpcInvoker> lookUp(String group, Invocation invocation);

    /**
     * 刷新节点信息
     * @param metadataSet all rpc node metadata of a provider with same micro-service-id and stage
     * @throws TomatoRpcException exception during refresh invoker data
     */
    void refresh(Set<MetaData> metadataSet) throws TomatoRpcException;

    /**
     * close micro-service space
     * @throws TomatoRpcException close exception
     */
    void close() throws TomatoRpcException;
}
